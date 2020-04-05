package client.room

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.ws.TextMessage
import akka.util.Timeout
import client.utils.MessageDictionary._
import common.Routes
import common.SharedRoom.{Room, RoomId}
import akka.pattern.ask
import akka.pattern.pipe
import client.{BasicActor, HttpClient}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

trait ClientRoom extends Room {


  /**
   * Open web socket with server room and try to join
   *
   * @return success if this room can be joined fail if the socket can't be opened or the room can't be joined
   */
  def join(): Future[Any]


  /**
   * Leave this room server side
   *
   * @return success if this room can be left fail otherwise
   */
  def leave(): Unit

  /**
   * Send a message to the server room
   *
   * @param msg the message to send
   */
  def send(msg: String): Unit


  /**
   * Callback that handle  message received from the server room
   *
   * @param callback callback to handle the message
   */
  def onMessageReceived(callback: String => Unit): Unit

  //TODO: implement this
  //def onStateChanged

}

object ClientRoom {
  def apply(innerActor: ActorRef, serverUri: String, roomId: RoomId): ClientRoom = ClientRoomImpl(innerActor, serverUri, roomId)

}

case class ClientRoomImpl(innerActor: ActorRef, serverUri: String, roomId: RoomId) extends ClientRoom {
  private implicit val timeout: Timeout = 5 seconds
  private implicit val executionContext = ExecutionContext.global


  override def join(): Future[Any] =
    (innerActor ? JoinRoom(roomId)) flatMap {
      case Success(_) => Future.successful()
      case Failure(ex) => Future.failed(ex)
    }

  override def leave(): Unit = innerActor ! LeaveRoom()

  override def send(msg: String): Unit = innerActor ! SendMsg(msg)

  override def onMessageReceived(callback: String => Unit): Unit = innerActor ! OnMsg(callback)
}

object ClientRoomActor {
  def apply(coreClient: ActorRef, serverUri: String, roomId: RoomId): Props =
    Props(classOf[ClientRoomActor], coreClient, serverUri, roomId)
}

case class ClientRoomActor(coreClient: ActorRef, serverUri: String, roomId: RoomId) extends BasicActor {
  private val httpClient = context.system actorOf HttpClient(serverUri)
  private var onMessageCallback: String => Unit = x => {}

  def waitSocketResponse(replyTo: ActorRef): Receive = {
    case HttpSocketFail(code) => replyTo ! Failure(new Exception(code.toString))
    case HttpSocketSuccess(outRef) =>
      /*context.become(socketOpened(outRef, replyTo))
      self ! SendMsg("join")*/ //TODO: protocol join
      replyTo ! Success()
      context.become(roomJoined(outRef))

    case OnMsg(callback) => onMessageCallback = callback
  }

  def socketOpened(outRef: ActorRef, replyTo: ActorRef): Receive = {
    case TextMessage.Strict("join ok") =>
      context.become(roomJoined(outRef))
      replyTo ! Success()
    case TextMessage.Strict("join fail") =>
      replyTo ! Failure(new Exception("Can't join"))

    case TextMessage.Strict(_) =>
      context.become(roomJoined(outRef))
      replyTo ! Success()

    case OnMsg(callback) => onMessageCallback = callback
  }

  def roomJoined(outRef: ActorRef): Receive = {
    case LeaveRoom() =>
      self ! SendMsg("leave")
      coreClient ! RoomLeaved(roomId)
      context.become(receive)
    case SendMsg(msg) =>
      outRef ! msg
    case TextMessage.Strict(msg) =>
      onMessageCallback(msg)

    case OnMsg(callback) => onMessageCallback = callback
  }

  override def receive: Receive = {
    case JoinRoom(roomId: RoomId) =>
      httpClient ! HttpSocketRequest(roomId)
      context.become(waitSocketResponse(sender))


  }
}


