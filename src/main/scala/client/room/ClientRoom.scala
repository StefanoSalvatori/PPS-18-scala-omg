package client.room

import akka.Done
import akka.pattern.ask
import akka.util.Timeout
import common.CommonRoom.{Room, RoomId}
import common.actors.ApplicationActorSystem

import scala.concurrent.Future
import scala.concurrent.duration._

trait ClientRoom extends Room {


  val serverUri: String

  /**
   * Open web socket with server room and try to join
   * @return success if this room can be joined fail if the socket can be opened or the room can't be joined
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
   * Callback that handle a message received from the server room
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

import common.actors.ApplicationActorSystem._
case class ClientRoomImpl(serverUri: String, roomId: RoomId) extends ClientRoom {
  private implicit val timeout : Timeout = 5 seconds

  private val socketHandler = SocketHandler(serverUri.replace("http://", ""), roomId)

  override def join(): Future[Unit] = {
    //open socket,
    // if successful try to join
    for {
      _ <- socketHandler.openSocket()
      _ <- socketHandler.join()
    } yield {
      Future.successful()
    }
  }

  //TODO: should we check if this room is joined?
  override def leave(): Unit =  socketHandler.leave()

  //TODO: should we check if this room is joined?
  override def send(msg: String): Unit = socketHandler.sendMessage(msg)

  //TODO: should we check if this room is joined?
  override def onMessageReceived(callback: String => Unit): Unit = this.socketHandler.onMessageReceived(callback)
}





