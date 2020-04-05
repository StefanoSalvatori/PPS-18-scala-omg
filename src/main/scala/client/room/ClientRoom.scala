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
  def apply(coreClient: ActorRef, httpServerUri: String, roomId: RoomId)(implicit system: ActorSystem): ClientRoom =
    ClientRoomImpl(coreClient, httpServerUri, roomId)

}

case class ClientRoomImpl(coreClient: ActorRef, httpServerUri: String, roomId: RoomId)
                         (implicit val system: ActorSystem)
  extends ClientRoom {
  private implicit val timeout: Timeout = 5 seconds
  private implicit val executionContext = ExecutionContext.global
  private implicit var innerActor: Option[ActorRef] = None

  override def join(): Future[Any] = {
    val ref = system actorOf ClientRoomActor(coreClient, httpServerUri, this)
    this.innerActor = Some(ref)
    (ref ? JoinRoom(roomId)) flatMap {
      case Success(_) => Future.successful()
      case Failure(ex) => Future.failed(ex)
    }
  }


  override def leave(): Unit = innerActor.foreach(_ ! LeaveRoom())

  override def send(msg: String): Unit = innerActor.foreach(_ ! SendMsg(msg))

  override def onMessageReceived(callback: String => Unit): Unit = innerActor.foreach(_ ! OnMsg(callback))
}






