package client.matchmake

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.pattern.ask
import akka.util.Timeout
import client.matchmake.MatchmakingActor.{JoinMatchmake, LeaveMatchmake}
import client.room.{ClientRoom, JoinedRoom}
import common.communication.CommunicationProtocol.SessionId
import common.room.Room.{RoomId, RoomType}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future, Promise}


trait ClientMatchmaker {

  /**
   * Join the serverside matchmaking queue for this type of room
   *
   * @param roomType matchmake room type to join
   * @return a future that completes sucessfully when the matchmaker server side creates the match
   */
  def joinMatchmake(roomType: RoomType): Future[JoinedRoom]

  /**
   * Leave the serverside matchmaking queue for this type of room serverside
   *
   * @param roomType matchmake room room type to leave
   */
  def leaveMatchmake(roomType: RoomType): Future[Unit]

}

object ClientMatchmaker {
  def apply(coreClient: ActorRef, httpServerUri: String)
           (implicit system: ActorSystem): ClientMatchmaker = new ClientMatchmakerImpl(coreClient, httpServerUri)
}


class ClientMatchmakerImpl(private val coreClient: ActorRef,
                           private val httpServerUri: String)
                          (implicit val system: ActorSystem) extends ClientMatchmaker {

  private implicit val timeout: Timeout = 5 seconds
  private implicit val executor: ExecutionContextExecutor = system.dispatcher

  private var matchmakeConnections: Map[RoomType, ActorRef] = Map()
  private var promises: Map[RoomType, Promise[JoinedRoom]] = Map()

  override def joinMatchmake(roomType: RoomType): Future[JoinedRoom] = {
    if (!this.matchmakeConnections.keySet.contains(roomType)) {
      val p = createPromise(roomType)
      val ref = system.actorOf(MatchmakingActor(roomType, this.httpServerUri))
      this.matchmakeConnections = this.matchmakeConnections.updated(roomType, ref)
      (ref ? JoinMatchmake).flatMap { res =>
        val (cId, rId) = res.asInstanceOf[(SessionId, RoomId)]
        val room = ClientRoom.createJoinable(coreClient, httpServerUri, rId, Set())
        removeMatchmakeConnection(roomType)
        p.completeWith(room.joinWithSessionId(cId)).future
      }
    } else {
      promises(roomType).future
    }
  }

  override def leaveMatchmake(roomType: RoomType): Future[Unit] = {
    this.matchmakeConnections.get(roomType) match {
      case Some(ref) =>
        (ref ? LeaveMatchmake).map { _ =>
          //make the join future fail
          this.promises(roomType).failure(new Exception("Matchmake left"))
          this.removeMatchmakeConnection(roomType)
        }
      case None => Future.failed(new Exception(""))
    }

  }

  private def removeMatchmakeConnection(roomType: RoomType) = {
    this.promises = this.promises - roomType
    this.matchmakeConnections(roomType) ! PoisonPill
    this.matchmakeConnections = this.matchmakeConnections - roomType
  }

  private def createPromise(roomType: RoomType): Promise[JoinedRoom] = {
    val p = Promise[JoinedRoom]()
    this.promises = this.promises.updated(roomType, p)
    p
  }
}




