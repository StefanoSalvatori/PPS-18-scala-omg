package client.room

import akka.actor.ActorSystem
import akka.util.Timeout
import common.Routes
import common.SharedRoom.{Room, RoomId}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

trait ClientRoom extends Room {


  val serverUri: String

  /**
   * Open web socket with server room and try to join
   *
   * @return success if this room can be joined fail if the socket can't be opened or the room can't be joined
   */
  def join(): Future[Unit]


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
  def apply(serverUri: String, roomId: RoomId): ClientRoom = ClientRoomImpl(serverUri, roomId)

}

case class ClientRoomImpl(serverUri: String, roomId: RoomId) extends ClientRoom {
  private implicit val timeout: Timeout = 5 seconds
  private implicit val executionContext = ExecutionContext.global
  private val roomSocket = RoomSocket(Routes.webSocketConnection(roomId))

  override def join(): Future[Unit] = {
    //open socket,
    // if successful try to join
    /*for {
      _ <- roomSocket.openSocket()
      _ <- roomSocket.sendJoin()
    } yield { }*/

    Future.successful()
  }

  //TODO: should we check if this room is joined?
  override def leave(): Unit = this.roomSocket.sendLeave()

  //TODO: should we check if this room is joined?
  override def send(msg: String): Unit = this.roomSocket.sendMessage(msg)

  //TODO: should we check if this room is joined?
  override def onMessageReceived(callback: String => Unit): Unit = this.roomSocket.onMessageReceived(callback)
}





