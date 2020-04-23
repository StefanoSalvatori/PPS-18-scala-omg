package client.room

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import client.utils.MessageDictionary.{SendJoin, SendReconnect}
import common.room.{Room, RoomProperty}
import common.room.Room.{RoomId, RoomPassword}

import scala.concurrent.Future
import scala.util.{Failure, Success}
import akka.pattern.ask
import common.communication.CommunicationProtocol.SessionId

/**
 * A room that can be joined
 */
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
  def joinWithSessionId(sessionId: SessionId, password: RoomPassword = Room.defaultPublicPassword): Future[JoinedRoom]

  /**
   * Open web socket with server room and try to reconnect to the room
   *
   * @return success if this room can be joined fail if the socket can't be opened or the room can't be joined
   */
  def reconnect(sessionId: SessionId, password: RoomPassword = Room.defaultPublicPassword): Future[JoinedRoom]
}

object JoinableRoom {
  def apply(roomId: RoomId,
            coreClient: ActorRef,
            httpServerUri: String,
            properties: Set[RoomProperty])
           (implicit system: ActorSystem): JoinableRoom =
    new JoinableRoomImpl(roomId, coreClient, httpServerUri, properties)

}

private class JoinableRoomImpl(override val roomId: RoomId,
                       private val coreClient: ActorRef,
                       private val httpServerUri: String,
                       override val properties: Set[RoomProperty])
                      (override implicit val system: ActorSystem)
  extends ClientRoomImpl(roomId, properties) with JoinableRoom {

  private var innerActor: Option[ActorRef] = None


  override def join(password: RoomPassword = Room.defaultPublicPassword): Future[JoinedRoom] = {
    this.joinFuture(None, password)
  }

  override def joinWithSessionId(sessionId: SessionId, password: RoomPassword): Future[JoinedRoom] = {
    this.joinFuture(Some(sessionId), password)
  }

  override def reconnect(sessionId: SessionId, password: RoomPassword): Future[JoinedRoom] = {
    val ref = this.spawnInnerActor()
    (ref ? SendReconnect(Some(sessionId), password)) flatMap {
      case Success(response) =>
        Future.successful(response.asInstanceOf[JoinedRoom])
      case Failure(ex) =>
        this.killInnerActor()
        Future.failed(ex)
    }  }

  private def joinFuture(sessionId: Option[SessionId], password: RoomPassword): Future[JoinedRoom] = {
    val ref = this.spawnInnerActor()
    (ref ? SendJoin(sessionId, password)) flatMap {
      case Success(response) =>
        Future.successful(response.asInstanceOf[JoinedRoom])
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
