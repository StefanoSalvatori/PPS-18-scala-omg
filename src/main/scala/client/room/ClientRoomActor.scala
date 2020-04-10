package client.room

import akka.actor.{ActorRef, Props, Stash}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message}
import client.utils.MessageDictionary._
import client.{BasicActor, HttpClient}
import common.room.SharedRoom.RoomId
import common.communication.CommunicationProtocol.ProtocolMessageType._
import common.communication.CommunicationProtocol.{ProtocolMessageType, RoomProtocolMessage}
import common.communication.{BinaryProtocolSerializer, SocketSerializer}

import scala.collection.immutable.Queue
import scala.util.{Failure, Success}

object ClientRoomActor {
  def apply[S](coreClient: ActorRef, serverUri: String, room: ClientRoom): Props =
    Props(classOf[ClientRoomActor[S]], coreClient, serverUri, room)
}

/**
 * Handles the connection with the server side room.
 * Notify the coreClient if the associated room was left or joined
 */
case class ClientRoomActor[S](coreClient: ActorRef, httpServerUri: String, room: ClientRoom) extends BasicActor with Stash {
  private val httpClient = context.system actorOf HttpClient(httpServerUri)
  private var onMessageCallback: Option[Any with java.io.Serializable => Unit] = None
  private var onStateChangedCallback: Option[Any with java.io.Serializable => Unit] = None

  override def receive: Receive = onReceive orElse fallbackReceive

  def waitSocketResponse(replyTo: ActorRef): Receive = onWaitSocketResponse(replyTo) orElse fallbackReceive

  def socketOpened(outRef: ActorRef, replyTo: ActorRef): Receive = onSocketOpened(outRef, replyTo) orElse fallbackReceive

  def roomJoined(outRef: ActorRef): Receive = onRoomJoined(outRef) orElse fallbackReceive


  //actor states
  def onReceive: Receive = {
    case SendJoin(roomId: RoomId) =>
      httpClient ! HttpSocketRequest(roomId, BinaryProtocolSerializer)
      context.become(waitSocketResponse(sender))

    case OnMsgCallback(callback) =>
      onMessageCallback = Some(callback)
      unstashAll()

    case OnStateChangedCallback(callback) =>
      onStateChangedCallback = Some(callback)
      unstashAll()
  }

  def onWaitSocketResponse(replyTo: ActorRef): Receive = {
    case RoomProtocolMessage => stash()

    case HttpSocketFail(code) =>
      replyTo ! Failure(new Exception(code.toString))
      context.become(receive)


    case HttpSocketSuccess(outRef) =>
      context.become(socketOpened(outRef, replyTo))
      unstashAll()
      self ! SendProtocolMessage(RoomProtocolMessage(JoinRoom))


    case OnMsgCallback(callback) =>
      onMessageCallback = Some(callback)
      unstashAll()

    case OnStateChangedCallback(callback) =>
      onStateChangedCallback = Some(callback)
      unstashAll()
  }

  def onSocketOpened(outRef: ActorRef, replyTo: ActorRef): Receive = {

    case RoomProtocolMessage(ProtocolMessageType.JoinOk, _, _) =>
      coreClient ! ClientRoomActorJoined
      replyTo ! Success()
      context.become(roomJoined(outRef))


    case RoomProtocolMessage(ProtocolMessageType.ClientNotAuthorized, _, _) =>
      replyTo ! Failure(new Exception("Can't join"))


    case SendProtocolMessage(msg) =>
      outRef ! msg

    case OnMsgCallback(callback) =>
      onMessageCallback = Some(callback)
      unstashAll()

    case OnStateChangedCallback(callback) =>
      onStateChangedCallback = Some(callback)
      unstashAll()
  }

  def onRoomJoined(outRef: ActorRef): Receive = {

    case RoomProtocolMessage(ProtocolMessageType.ClientNotAuthorized, _, _) =>

    case RoomProtocolMessage(ProtocolMessageType.Tell, _, payload) => handleMessageReceived(payload)

    case RoomProtocolMessage(ProtocolMessageType.Broadcast, _, payload) => handleMessageReceived(payload)

    case RoomProtocolMessage(ProtocolMessageType.StateUpdate, _, payload) => handleStateChangedReceived(payload)

    case SendLeave =>
      self ! SendProtocolMessage(RoomProtocolMessage(LeaveRoom))
      coreClient ! ClientRoomActorLeaved
      sender ! Success()
      context.become(receive)

    case SendStrictMessage(msg: Any with java.io.Serializable) =>
      self ! SendProtocolMessage(RoomProtocolMessage(MessageRoom, "", msg))

    case SendProtocolMessage(msg) =>
      outRef ! msg

    case OnMsgCallback(callback) =>
      onMessageCallback = Some(callback)
      unstashAll()

    case OnStateChangedCallback(callback) =>
      onStateChangedCallback = Some(callback)
      unstashAll()

    case RetrieveClientRoom => sender ! ClientRoomResponse(this.room)
  }


  //private utilities

  private def handleMessageReceived(msg: Any with java.io.Serializable) = {
    //stash messages if callback is not defined
    //They will be handled as soon as the callback is defined
    this.onMessageCallback match {
      case Some(callback) => callback(msg)
      case None => stash()
    }
  }

  private def handleStateChangedReceived(state: Any with java.io.Serializable) = {
    //stash messages if callback is not defined
    //They will be handled as soon as the callback is defined
    this.onStateChangedCallback match {
      case Some(callback) => callback(state)
      case None => stash()
    }
  }


}