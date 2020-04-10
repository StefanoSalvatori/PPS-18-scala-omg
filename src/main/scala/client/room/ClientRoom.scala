package client.room

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.util.Timeout
import client.utils.MessageDictionary._
import common.room.SharedRoom.{BasicRoom, RoomId}
import akka.pattern.ask
import common.room.{BooleanRoomPropertyValue, DoubleRoomPropertyValue, IntRoomPropertyValue, RoomPropertyValue, StringRoomPropertyValue}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

trait ClientRoom extends BasicRoom {

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
  def leave(): Future[Any]

  /**
   * Send a message to the server room
   *
   * @param msg the message to send
   */
  def send(msg: Any with java.io.Serializable): Unit


  /**
   * Callback that handle  message received from the server room
   *
   * @param callback callback to handle the message
   */
  def onMessageReceived(callback: Any => Unit): Unit


  /**
   * Callback that handle message of game state changed received from the server room
   *
   * @param callback callback to handle the change of state
   */
  def onStateChanged(callback: Any with java.io.Serializable => Unit): Unit

  /**
   * Properties of the room.
   *
   * @return a map containing property names as keys (name -> value)
   */
  def properties: Map[String, Any]

  /**
   * Getter of the value of a given property
   * @param propertyName the name of the property
   * @return the value of the property, as instance of first class values (Int, String, Boolean. Double)
   */
  def valueOf(propertyName: String): Any
}

object ClientRoom {
  def apply(coreClient: ActorRef, httpServerUri: String, roomId: RoomId, properties: Map[String, RoomPropertyValue])
           (implicit system: ActorSystem): ClientRoom =
    ClientRoomImpl(coreClient, httpServerUri, roomId, properties)
}

case class ClientRoomImpl(coreClient: ActorRef,
                          httpServerUri: String,
                          override val roomId: RoomId,
                          private val _properties: Map[String, RoomPropertyValue])
                         (implicit val system: ActorSystem) extends ClientRoom {

  private implicit val timeout: Timeout = 5 seconds
  private implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global
  private implicit var innerActor: Option[ActorRef] = None

  override def properties: Map[String, Any] = {
    def runtimeValue(value: RoomPropertyValue): Any = value match {
      case v: IntRoomPropertyValue => v.value
      case v: StringRoomPropertyValue => v.value
      case v: BooleanRoomPropertyValue => v.value
      case v: DoubleRoomPropertyValue => v.value
    }

    _properties.map(e => (e._1, runtimeValue(e._2)))
  }

  override def valueOf(propertyName: String): Any = RoomPropertyValue runtimeValue _properties(propertyName)

  override def join(): Future[Any] = {
    val ref = this.spawnInnerActor()
    (ref ? SendJoin(roomId)) flatMap {
      case Success(_) => Future.successful()
      case Failure(ex) =>
        this.killInnerActor()
        Future.failed(ex)
    }
  }


  override def leave(): Future[Any] =
    innerActor match {
      case Some(value) =>
        value ? SendLeave flatMap {
          case Success(_) => Future.successful()
          case Failure(ex) => Future.failed(ex)
        }
      case None => Future.failed(new Exception("You must join a room before leaving"))
    }

  override def send(msg: Any with java.io.Serializable): Unit = innerActor.foreach(_ ! SendStrictMessage(msg))

  override def onMessageReceived(callback: Any => Unit): Unit = innerActor.foreach(_ ! OnMsgCallback(callback))

  override def onStateChanged(callback: Any with java.io.Serializable => Unit): Unit = innerActor.foreach(_ ! OnStateChangedCallback(callback))

  private def spawnInnerActor(): ActorRef = {
    val ref = system actorOf ClientRoomActor(coreClient, httpServerUri, this)
    this.innerActor = Some(ref)
    ref
  }

  private def killInnerActor(): Unit = {
    this.innerActor match {
      case Some(value) => value ! PoisonPill
      case None =>
    }
  }

}






