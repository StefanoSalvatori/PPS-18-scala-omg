package client.room

import akka.actor.{ActorRef, ActorSystem}
import client.utils.MessageDictionary._
import common.room.Room.RoomId
import common.room.RoomProperty

import scala.concurrent.Future
import scala.util.{Failure, Success}
import akka.pattern.ask
import common.communication.CommunicationProtocol.{SocketSerializable, SessionId}

/**
 * Room that is joined
 */
trait JoinedRoom extends ClientRoom {
  /**
   * @return the session id of this client associated to the room.
   */
  def sessionId: SessionId


  /**
   * Leave this room server side
   *
   * @return success if this room can be left fail otherwise
   */
  def leave(): Future[Any]

  /**
   * Send a message to the server room
   *
   * @param msg the message to send
   */
  def send(msg: SocketSerializable): Unit

  /**
   * Callback that handle  message received from the server room
   *
   * @param callback callback to handle the message
   */
  def onMessageReceived(callback: Any => Unit): Unit

  /**
   * This event is triggered when the server updates its state.
   *
   * @param callback callback to handle the change of state
   */
  def onStateChanged(callback: Any => Unit): Unit

  /**
   * This event is triggered when the room is closed
   *
   * @param callback callback to handle the event
   */
  def onClose(callback: => Unit): Unit

  /**
   * This event is triggered when an error occurs
   *
   * @param callback callback to handle the event
   */
  def onError(callback: Throwable => Unit): Unit


}

object JoinedRoom {
  def apply(innerActor: ActorRef, sessionId: SessionId, roomId: RoomId, properties: Set[RoomProperty])
           (implicit system: ActorSystem): JoinedRoom =
    new JoinedRoomImpl(innerActor, sessionId, roomId, properties)
}


private class JoinedRoomImpl(private val innerActor: ActorRef,
                     val sessionId: SessionId,
                     override val roomId: RoomId,
                     override val properties: Set[RoomProperty])
                    (override implicit val system: ActorSystem)
  extends ClientRoomImpl(roomId, properties) with JoinedRoom {


  override def leave(): Future[Any] =
    this.innerActor ? SendLeave flatMap {
      case Success(_) => Future.successful()
      case Failure(ex) => Future.failed(ex)
    }

  override def send(msg: SocketSerializable): Unit = this.innerActor ! SendStrictMessage(msg)

  override def onMessageReceived(callback: Any => Unit): Unit = this.innerActor ! OnMsgCallback(callback)

  override def onStateChanged(callback: Any => Unit): Unit = this.innerActor ! OnStateChangedCallback(callback)

  override def onClose(callback: => Unit): Unit = this.innerActor ! OnCloseCallback(() => callback)

  override def onError(callback: Throwable => Unit): Unit = this.innerActor ! OnErrorCallback(callback)
}