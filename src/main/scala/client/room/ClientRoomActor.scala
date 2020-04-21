package client.room

import akka.actor.{ActorRef, Props, Stash}
import client.utils.MessageDictionary._
import client.{BasicActor, HttpClient}
import common.communication.BinaryProtocolSerializer
import common.communication.CommunicationProtocol.ProtocolMessageType._
import common.communication.CommunicationProtocol.{ProtocolMessageType, RoomProtocolMessage, SocketSerializable, SessionId}
import common.room.Room.RoomPassword

import scala.util.{Failure, Success}

sealed trait ClientRoomActor extends BasicActor


object ClientRoomActor {
  def apply(coreClient: ActorRef, serverUri: String, room: ClientRoom): Props =
    Props(classOf[ClientRoomActorImpl], coreClient, serverUri, room)
}

/**
 * Handles the connection with the server side room.
 * Notify the coreClient if the associated room is left or joined.
 */
case class ClientRoomActorImpl(coreClient: ActorRef, httpServerUri: String, room: ClientRoom) extends ClientRoomActor with Stash {
  private val httpClient = context.system actorOf HttpClient(httpServerUri)
  private var onMessageCallback: Option[Any => Unit] = None
  private var onStateChangedCallback: Option[Any => Unit] = None
  private var onCloseCallback: Option[() => Unit] = None
  private var onErrorCallback: Option[Throwable => Unit] = None

  private var joinPassword: RoomPassword = _
  private var joinedRoom : JoinedRoom = _

  override def receive: Receive = waitJoinRequest orElse callbackDefinition orElse handleErrors orElse fallbackReceive

  def waitSocketResponse(replyTo: ActorRef, sessionId: Option[SessionId]): Receive =
    onWaitSocketResponse(replyTo, sessionId) orElse callbackDefinition orElse handleErrors orElse fallbackReceive

  def socketOpened(outRef: ActorRef, replyTo: ActorRef): Receive =
    onSocketOpened(outRef, replyTo) orElse
      callbackDefinition orElse
      heartbeatResponse(outRef) orElse
      handleErrors orElse
      fallbackReceive

  def roomJoined(outRef: ActorRef): Receive =
    onRoomJoined(outRef) orElse
      callbackDefinition orElse
      heartbeatResponse(outRef) orElse
      handleErrors orElse
      fallbackReceive

  def waitLeaveResponse(replyTo: ActorRef, outRef: ActorRef): Receive =
    onWaitLeaveResponse(replyTo, outRef) orElse
      callbackDefinition orElse
      heartbeatResponse(outRef) orElse
      handleErrors orElse
      fallbackReceive

  //actor states
  def waitJoinRequest: Receive = {
    case SendJoin(sessionId: Option[SessionId], password: RoomPassword) =>
      joinPassword = password
      httpClient ! HttpSocketRequest(this.room.roomId, BinaryProtocolSerializer())
      context.become(waitSocketResponse(sender, sessionId))
  }

  def onWaitSocketResponse(replyTo: ActorRef, sessionId: Option[SessionId]): Receive = {
    case RoomProtocolMessage => stash()

    case HttpSocketFail(code) =>
      replyTo ! Failure(new Exception(code.toString))
      context.become(receive)

    case HttpSocketSuccess(outRef) =>
      context.become(socketOpened(outRef, replyTo))
      unstashAll()
      outRef ! RoomProtocolMessage(
        messageType = JoinRoom,
        sessionId = sessionId.getOrElse(SessionId.empty),
        payload = joinPassword)
  }

  def onSocketOpened(outRef: ActorRef, replyTo: ActorRef): Receive = {
    case RoomProtocolMessage(JoinOk, sessionId, _) =>
      coreClient ! ClientRoomActorJoined
      this.joinedRoom = JoinedRoom(self, sessionId, room.roomId, room.properties)
      replyTo ! Success(this.joinedRoom)
      context.become(roomJoined(outRef))
      unstashAll()

    case RoomProtocolMessage(ClientNotAuthorized, _, _) =>
      replyTo ! Failure(new Exception("Can't join"))

    case SendStrictMessage(_: SocketSerializable) => stash()
    case RoomProtocolMessage(Tell, _, _) => stash()
    case RoomProtocolMessage(Broadcast, _, _) => stash()
    case RoomProtocolMessage(RoomClosed, _, _) => stash()

  }

  def onRoomJoined(outRef: ActorRef): Receive = {
    case RoomProtocolMessage(ClientNotAuthorized, _, _) =>
    case RoomProtocolMessage(Tell, _, payload) => handleMessageReceived(payload)
    case RoomProtocolMessage(Broadcast, _, payload) => handleMessageReceived(payload)
    case RoomProtocolMessage(StateUpdate, _, payload) => handleStateChangedReceived(payload)
    case RoomProtocolMessage(RoomClosed, _, _) =>
      this.onCloseCallback match {
        case Some(value) => value()
        case None => stash()
      }

    case SendLeave =>
      outRef ! RoomProtocolMessage(LeaveRoom)
      context.become(waitLeaveResponse(sender, outRef))

    case SendStrictMessage(msg: SocketSerializable) =>
      outRef ! RoomProtocolMessage(MessageRoom, "", msg)

    case RetrieveClientRoom => sender ! ClientRoomResponse(this.joinedRoom)
  }

  def onWaitLeaveResponse(replyTo: ActorRef, outRef: ActorRef): Receive = {
    case RoomProtocolMessage(ProtocolMessageType.LeaveOk, _, _) =>
      coreClient ! ClientRoomActorLeft
      replyTo ! Success()
      context.become(receive)

    case RoomProtocolMessage(ProtocolMessageType.ClientNotAuthorized, _, _) =>
      replyTo ! Failure(new Exception("Can't leave"))
      context.become(roomJoined(outRef))
  }

  //private utilities

  private def handleErrors: Receive = {
    case SocketError(ex) =>
      onErrorCallback match {
        case Some(value) => value(ex)
        case None => stash()
      }

  }

  private def heartbeatResponse(roomSocket: ActorRef): Receive = {
    case RoomProtocolMessage(Ping, _, _) => roomSocket ! RoomProtocolMessage(Pong)
  }

  private def callbackDefinition: Receive = {
    case OnMsgCallback(callback) =>
      onMessageCallback = Some(callback)
      unstashAll()

    case OnStateChangedCallback(callback) =>
      onStateChangedCallback = Some(callback)
      unstashAll()

    case OnCloseCallback(callback) =>
      onCloseCallback = Some(callback)
      unstashAll()

    case OnErrorCallback(callback) =>
      onErrorCallback = Some(callback)
      unstashAll()
  }

  private def handleMessageReceived(msg: Any): Unit = {
    handleIfDefinedOrStash(this.onMessageCallback, msg)
  }

  private def handleStateChangedReceived(state: Any): Unit = {
    handleIfDefinedOrStash(this.onStateChangedCallback, state)
  }

  //stash messages if callback is not defined
  //They will be handled as soon as the callback is defined
  private def handleIfDefinedOrStash(callback: Option[Any => Unit], msg: Any): Unit = {
    callback match {
      case Some(value) => value(msg)
      case None => stash()
    }
  }




}

