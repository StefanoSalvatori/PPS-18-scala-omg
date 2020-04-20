package client.room

import java.io
import java.util.NoSuchElementException

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.util.Timeout
import client.utils.MessageDictionary._
import common.room.Room.{BasicRoom, RoomId, RoomPassword}
import akka.pattern.ask
import common.room.{NoSuchPropertyException, Room, RoomProperty, RoomPropertyValue}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}


trait JoinableRoom extends ClientRoom {
  /**
   * Open web socket with server room and try to join
   *
   * @return success if this room can be joined fail if the socket can't be opened or the room can't be joined
   */
  def join(password: RoomPassword = Room.defaultPublicPassword): Future[JoinedRoom]

  /**
   * Open web socket with server room and try to join with the given session id
   *
   * @return success if this room can be joined fail if the socket can't be opened or the room can't be joined
   */
  def joinWithSessionId(sessionId: String, password: RoomPassword = Room.defaultPublicPassword): Future[JoinedRoom]
}

trait JoinedRoom extends ClientRoom {
  /**
   * @return if present the session id of this client when the room is joined.
   */
  def sessionId: String


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

trait ClientRoom extends BasicRoom {

}

class ClientRoomImpl(override val roomId: RoomId,
                     override val properties: Set[RoomProperty])
                    (implicit val system: ActorSystem) extends ClientRoom {
  protected implicit val timeout: Timeout = 5 seconds
  protected implicit val executionContext: ExecutionContextExecutor = system.dispatcher
}

class JoinedRoomImpl(private val innerActor: ActorRef,
                     val sessionId: String,
                     override val roomId: RoomId,
                     override val properties: Set[RoomProperty])
                    (override implicit val system: ActorSystem)
  extends ClientRoomImpl(roomId, properties) with JoinedRoom {


  override def leave(): Future[Any] =
    this.innerActor ? SendLeave flatMap {
      case Success(_) => Future.successful()
      case Failure(ex) => Future.failed(ex)
    }

  override def send(msg: Any with java.io.Serializable): Unit = this.innerActor ! SendStrictMessage(msg)

  override def onMessageReceived(callback: Any => Unit): Unit = this.innerActor ! OnMsgCallback(callback)

  override def onStateChanged(callback: Any => Unit): Unit = this.innerActor ! OnStateChangedCallback(callback)

  override def onClose(callback: => Unit): Unit = this.innerActor ! OnCloseCallback(() => callback)

  override def onError(callback: Throwable => Unit): Unit = this.innerActor ! OnErrorCallback(callback)
}

class JoinableRoomImpl(override val roomId: RoomId,
                       private val coreClient: ActorRef,
                       private val httpServerUri: String,
                       override val properties: Set[RoomProperty])
                      (override implicit val system: ActorSystem)
  extends ClientRoomImpl(roomId, properties) with JoinableRoom {

  private var innerActor: Option[ActorRef] = None


  override def join(password: RoomPassword = Room.defaultPublicPassword): Future[JoinedRoom] = {
    this.joinFuture(None, password)

  }

  override def joinWithSessionId(sessionId: String, password: RoomPassword): Future[JoinedRoom] = {
    this.joinFuture(Some(sessionId), password)
  }

  private def joinFuture(sessionId: Option[String], password: RoomPassword) = {
    val ref = this.spawnInnerActor()
    (ref ? SendJoin(sessionId, password)) flatMap {
      case Success(responseId) =>
        Future.successful(responseId.asInstanceOf[JoinedRoom])
      case Failure(ex) =>
        this.killInnerActor()
        Future.failed(ex)
    }
  }


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


object ClientRoom {
  def apply(coreClient: ActorRef, httpServerUri: String, roomId: RoomId, properties: Set[RoomProperty])
           (implicit system: ActorSystem): JoinableRoom =
    new JoinableRoomImpl(roomId, coreClient, httpServerUri, properties)
}

/*
case class ClientRoomImpl(private val coreClient: ActorRef,
                          private val httpServerUri: String,
                          override val roomId: RoomId,
                          private val _properties: Map[String, RoomPropertyValue],
                          private var _sessionId: Option[String])
                         (implicit val system: ActorSystem) extends ClientRoom {

  private implicit val timeout: Timeout = 5 seconds
  private implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  private implicit var innerActor: Option[ActorRef] = None


  private var onMessageCallback: Option[Any => Unit] = None
  private var onStateChangedCallback: Option[Any => Unit] = None
  private var onCloseCallback: Option[() => Unit] = None
  private var onErrorCallback: Option[Throwable => Unit] = None

  override def properties: Map[String, Any] = _properties.map(e => (e._1, RoomPropertyValue valueOf e._2))

  override def valueOf(propertyName: String): Any =
    tryReadingProperty(propertyName)(p => RoomPropertyValue valueOf _properties(p))

  override def propertyOf(propertyName: String): RoomProperty =
    tryReadingProperty(propertyName)(p => RoomProperty(p, _properties(p)))

  override def sessionId: Option[String] = this._sessionId

  override def join(password: RoomPassword = Room.defaultPublicPassword): Future[Any] = {
    val ref = this.spawnInnerActor()
    (ref ? SendJoin(sessionId, password)) flatMap {
      case Success(responseId) =>
        this._sessionId = Some(responseId.asInstanceOf[String])
        Future.successful()
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

  override def onMessageReceived(callback: Any => Unit): Unit =
    this.innerActor match {
      case Some(ref) => ref ! OnMsgCallback(callback)
      case None => this.onMessageCallback = Some(callback)
    }

  override def onStateChanged(callback: Any => Unit): Unit =
    this.innerActor match {
      case Some(ref) => ref ! OnStateChangedCallback(callback)
      case None => this.onStateChangedCallback = Some(callback)
    }

  override def onClose(callback: => Unit): Unit =
    this.innerActor match {
      case Some(ref) => ref ! OnCloseCallback(() => callback)
      case None => this.onCloseCallback = Some(() => callback)
    }

  override def onError(callback: Throwable => Unit): Unit =
    this.innerActor match {
      case Some(ref) => ref ! OnErrorCallback(callback)
      case None => this.onErrorCallback = Some(callback)
    }


  private def spawnInnerActor(): ActorRef = {
    val ref = system actorOf ClientRoomActor(coreClient, httpServerUri, this)
    //if callbacks were defined before actor spawning we set them now
    this.sendCallbacksToActor(ref)
    this.innerActor = Some(ref)
    ref
  }


  private def killInnerActor(): Unit = {
    this.innerActor match {
      case Some(value) => value ! PoisonPill
      case None =>
    }
  }

  private def sendCallbacksToActor(ref: ActorRef): Unit = {
    this.onCloseCallback.foreach(ref ! OnCloseCallback(_))
    this.onStateChangedCallback.foreach(ref ! OnStateChangedCallback(_))
    this.onMessageCallback.foreach(ref ! OnMsgCallback(_))
    this.onErrorCallback.foreach(ref ! OnErrorCallback(_))

  }

  private def tryReadingProperty[T](propertyName: String)(f: Function[String, T]): T = try {
    f(propertyName)
  } catch {
    case _: NoSuchElementException => throw NoSuchPropertyException()
  }

}
*/





