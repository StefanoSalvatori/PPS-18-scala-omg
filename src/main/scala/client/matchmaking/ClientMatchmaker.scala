package client.matchmaking

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.pattern.ask
import akka.util.Timeout
import client.matchmaking.MatchmakingActor.{JoinMatchmake, LeaveMatchmake}
import client.room.{ClientRoom, JoinedRoom}
import common.communication.CommunicationProtocol.{MatchmakeTicket, SessionId}
import common.room.Room.{RoomId, RoomType}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future, Promise}
import scala.util.{Failure, Success}


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
  def leaveMatchmake(roomType: RoomType): Future[Any]

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

  //keeps track of active matchmake connections
  private var matchmakeConnections: Map[RoomType, ActorRef] = Map()


  private var promises: Map[RoomType, Promise[JoinedRoom]] = Map()

  override def joinMatchmake(roomType: RoomType): Future[JoinedRoom] = {
    if (!this.matchmakeConnections.keySet.contains(roomType)) {
      val p = createPromise(roomType)
      val ref = system.actorOf(MatchmakingActor(roomType, this.httpServerUri))
      this.matchmakeConnections = this.matchmakeConnections.updated(roomType, ref)
      p.completeWith(createJoinMatchmakeFuture(ref, roomType)).future

    } else {
      promises(roomType).future
    }
  }

  override def leaveMatchmake(roomType: RoomType): Future[Any] = {
    this.matchmakeConnections.get(roomType) match {
      case Some(ref) =>
        (ref ? LeaveMatchmake).map { _ =>

          //make the join future fail
          this.promises(roomType).failure(new Exception("Matchmake left"))
          this.removeMatchmakeConnection(roomType)
        }
      case None => Future.successful()
    }

  }

  /**
   * Creates a future that completes when matchmake completes and the room is joined
   */
  private def createJoinMatchmakeFuture(matchmakeActor: ActorRef, roomType: RoomType): Future[JoinedRoom] = {
    (matchmakeActor ? JoinMatchmake).flatMap {
      case Success(res) =>
        val ticket = res.asInstanceOf[MatchmakeTicket]
        val room = ClientRoom.createJoinable(coreClient, httpServerUri, ticket.roomId, Set())
        removeMatchmakeConnection(roomType)
        room.joinWithSessionId(ticket.sessionId)
      case Failure(ex) =>
        Future.failed(ex)
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




