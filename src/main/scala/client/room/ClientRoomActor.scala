package client.room

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.model.ws.Message
import client.utils.MessageDictionary._
import client.{BasicActor, HttpClient}
import common.SharedRoom.RoomId
import common.communication.CommunicationProtocol.ProtocolMessageType._
import common.communication.CommunicationProtocol.{ProtocolMessageType, RoomProtocolMessage}
import common.communication.{BinaryProtocolSerializer, SocketSerializer}

import scala.util.{Failure, Success}

object ClientRoomActor {
  def apply(coreClient: ActorRef, serverUri: String, room: ClientRoom): Props =
    Props(classOf[ClientRoomActor], coreClient, serverUri, room)
}

/**
 * Handles the connection with the server side room.
 * Notify the coreClient if the associated room was left or joined
 */
case class ClientRoomActor(coreClient: ActorRef, httpServerUri: String, room: ClientRoom) extends BasicActor {
  private val httpClient = context.system actorOf HttpClient(httpServerUri)
  private var onMessageCallback: Any => Unit = x => {}
  private val parser: SocketSerializer[RoomProtocolMessage] = BinaryProtocolSerializer

  override def receive: Receive = onReceive orElse fallbackReceive

  def waitSocketResponse(replyTo: ActorRef): Receive = onWaitSocketResponse(replyTo) orElse fallbackReceive

  def socketOpened(outRef: ActorRef, replyTo: ActorRef): Receive = onSocketOpened(outRef, replyTo) orElse fallbackReceive

  def roomJoined(outRef: ActorRef): Receive = onRoomJoined(outRef) orElse fallbackReceive


  def onReceive: Receive = {
    case SendJoin(roomId: RoomId) =>
      httpClient ! HttpSocketRequest(roomId)
      context.become(waitSocketResponse(sender))
  }

  def onWaitSocketResponse(replyTo: ActorRef): Receive = {
    case HttpSocketFail(code) => replyTo ! Failure(new Exception(code.toString))
    case HttpSocketSuccess(outRef) =>
      context.become(socketOpened(outRef, replyTo))
      self ! SendProtocolMessage(parser.prepareToSocket(RoomProtocolMessage(JoinRoom)))
    case OnMsg(callback) => onMessageCallback = callback
  }


  def onSocketOpened(outRef: ActorRef, replyTo: ActorRef): Receive = {
    case msg: Message => parseMessage(msg)
    case RoomProtocolMessage(ProtocolMessageType.JoinOk, _, _) =>
      context.become(roomJoined(outRef))
      coreClient ! ClientRoomActorJoined(self)
      replyTo ! Success()
    case RoomProtocolMessage(ProtocolMessageType.ClientNotAuthorized, _, _) =>
      replyTo ! Failure(new Exception("Can't join"))
    case RoomProtocolMessage(ProtocolMessageType.Tell, _, payload) =>
      this.onMessageCallback(payload)
    case RoomProtocolMessage(ProtocolMessageType.Broadcast, _, payload) =>
      this.onMessageCallback(payload)
    case SendProtocolMessage(msg) =>
      outRef ! msg
    case OnMsg(callback) => onMessageCallback = callback
  }

  def onRoomJoined(outRef: ActorRef): Receive = {
    case msg: Message => parseMessage(msg)
    case RoomProtocolMessage(ProtocolMessageType.ClientNotAuthorized, _, _) =>
    case RoomProtocolMessage(ProtocolMessageType.Tell, _, payload) =>
      this.onMessageCallback(payload)
    case RoomProtocolMessage(ProtocolMessageType.Broadcast, _, payload) =>
      this.onMessageCallback(payload)
    case SendLeave =>
      self ! SendProtocolMessage(parser.prepareToSocket(RoomProtocolMessage(LeaveRoom)))
      coreClient ! ClientRoomActorLeaved(self)
      sender ! Success()
      context.become(receive)
    case SendStrictMessage(msg: Any) =>
      self ! SendProtocolMessage(parser.prepareToSocket(RoomProtocolMessage(MessageRoom, "", msg)))
    case SendProtocolMessage(msg) =>
      outRef ! msg
    case OnMsg(callback) => onMessageCallback = callback
    case RetrieveClientRoom => sender ! ClientRoomResponse(this.room)
  }

  private def parseMessage(msg: Message): Unit = {
    this.parser.parseFromSocket(msg) match {
      case Success(x) => self ! x
    }

  }

}