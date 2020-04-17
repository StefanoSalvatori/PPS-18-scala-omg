package client.room

import akka.actor.{ActorRef, Props, Stash}
import client.utils.MessageDictionary._
import client.{BasicActor, HttpClient}
import common.room.Room.{RoomId, RoomPassword}
import common.communication.BinaryProtocolSerializer
import common.communication.CommunicationProtocol.ProtocolMessageType._
import common.communication.CommunicationProtocol.{ProtocolMessageType, RoomProtocolMessage}

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
case class ClientRoomActorImpl(coreClient: ActorRef, httpServerUri: String, room: ClientRoom) extends ClientRoomActor
  with Stash {
  private val httpClient = context.system actorOf HttpClient(httpServerUri)
  private var onMessageCallback: Option[Any => Unit] = None
  private var onStateChangedCallback: Option[Any => Unit] = None
  private var onCloseCallback: Option[() => Unit] = None

  private var joinPassword: RoomPassword = _

  override def receive: Receive = waitJoinRequest orElse callbackDefinition /*orElse fallbackReceive*/

  def waitSocketResponse(replyTo: ActorRef, sessionId: Option[String]): Receive =
    onWaitSocketResponse(replyTo, sessionId) orElse callbackDefinition /*orElse fallbackReceive*/

  def socketOpened(outRef: ActorRef, replyTo: ActorRef): Receive =
    onSocketOpened(outRef, replyTo) orElse callbackDefinition /*orElse fallbackReceive*/

  def roomJoined(outRef: ActorRef): Receive =
    onRoomJoined(outRef) orElse callbackDefinition orElse fallbackReceive

  def waitLeaveResponse(replyTo: ActorRef, outRef: ActorRef): Receive =
    onWaitLeaveResponse(replyTo, outRef) orElse callbackDefinition /*orElse fallbackReceive*/

  //actor states
  def waitJoinRequest: Receive = {
    case SendJoin(sessionId: Option[String], password: RoomPassword) =>
      joinPassword = password
      httpClient ! HttpSocketRequest(this.room.roomId, BinaryProtocolSerializer())
      context.become(waitSocketResponse(sender, sessionId))
  }

  def onWaitSocketResponse(replyTo: ActorRef, sessionId: Option[String]): Receive = {
    case RoomProtocolMessage => stash()

    case HttpSocketFail(code) =>
      replyTo ! Failure(new Exception(code.toString))
      context.become(receive)

    case HttpSocketSuccess(outRef) =>
      context.become(socketOpened(outRef, replyTo))
      unstashAll()
      val stringId: String = sessionId match {
        case Some(value) => value
        case None => "" //empty string if no id is specified
      }
      outRef ! RoomProtocolMessage(
        messageType = JoinRoom,
        sessionId = stringId,
        payload = joinPassword)
  }

  def onSocketOpened(outRef: ActorRef, replyTo: ActorRef): Receive = {


    case RoomProtocolMessage(JoinOk, sessionId, _) =>
      coreClient ! ClientRoomActorJoined
      replyTo ! Success(sessionId)
      context.become(roomJoined(outRef))
      unstashAll()


    case RoomProtocolMessage(ClientNotAuthorized, _, _) =>
      replyTo ! Failure(new Exception("Can't join"))

    case SendStrictMessage(_: Any with java.io.Serializable) => stash()

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

    case SendStrictMessage(msg: Any with java.io.Serializable) =>
      outRef ! RoomProtocolMessage(MessageRoom, "", msg)

    case RetrieveClientRoom => sender ! ClientRoomResponse(this.room)
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
  }

  private def handleMessageReceived(msg: Any): Unit = {
    //stash messages if callback is not defined
    //They will be handled as soon as the callback is defined
    handleIfDefinedOrStash(this.onMessageCallback, msg)

  }

  private def handleStateChangedReceived(state: Any): Unit = {
    //stash messages if callback is not defined
    //They will be handled as soon as the callback is defined
    handleIfDefinedOrStash(this.onStateChangedCallback, state)
  }

  private def handleIfDefinedOrStash(callback: Option[Any => Unit], msg: Any): Unit = {
    callback match {
      case Some(value) => value(msg)
      case None => stash()
    }
  }


}

