package client

import akka.actor.{ActorRef, Stash}
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout
import client.room.{ClientRoom, ClientRoomActor}
import client.utils.MessageDictionary._
import common.RoomJsonSupport
import common.SharedRoom.{Room, RoomId}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}


sealed trait CoreClient extends BasicActor

object CoreClient {

  import akka.actor.Props

  def apply(serverUri: String): Props = Props(classOf[CoreClientImpl], serverUri)
}

class CoreClientImpl(private val serverUri: String) extends CoreClient with RoomJsonSupport with Stash {

  private implicit val timeout: Timeout = 5 seconds
  private val httpClient = context.system actorOf HttpClient(serverUri)

  private var joinedRooms: Set[ClientRoom] = Set()

  override def receive: Receive = onReceive orElse fallbackReceive

  def waitRoomsToJoin(replyTo: ActorRef): Receive = onWaitRoomsToJoin(replyTo) orElse fallbackReceive


  val onReceive: Receive = {

    case RoomSequenceResponse(rooms) =>
      sender ! Success(rooms.map(r => ClientRoom(self, serverUri, r.roomId)))

    case FailResponse(ex) => sender ! Failure(ex)

    case CreatePublicRoom(roomType, roomOptions) =>
      context.become(this.waitRoomsToJoin(sender))
      this.httpClient ! HttpPostRoom(roomType, roomOptions)

    case GetAvailableRooms(roomType, roomOptions) =>
      (this.httpClient ? HttpGetRooms(roomType, roomOptions)).map {
        case RoomSequenceResponse(rooms) => Success(rooms.map(r => ClientRoom(self, serverUri, r.roomId)))
        case FailResponse(ex) => Failure(ex)
      } pipeTo sender

    case Join(roomType, roomOptions) =>
      this.httpClient ! HttpGetRooms(roomType, roomOptions)
      context.become(this.waitRoomsToJoin(sender))

    case JoinById(roomId) =>
      val replyTo = sender
      println("join by id")
      println(this.joinedRooms)
      this.joinedRooms.find(_.roomId == roomId) match {
        case Some(_) =>
          replyTo ! Failure(new Exception("Room already joined"))
        case None =>
          context.become(this.waitRoomsToJoin(replyTo))
          self ! RoomResponse(Room(roomId))
      }

    case GetJoinedRooms =>
      sender ! JoinedRooms(this.joinedRooms)

    case RoomLeaved(roomId) =>
      this.joinedRooms.find(_.roomId == roomId) foreach {
        elem => this.joinedRooms = this.joinedRooms - elem
      }
  }


  /**
   * Behavior to handle response from HttpClientActor
   */
  def onWaitRoomsToJoin(replyTo: ActorRef): Receive = {

    case FailResponse(ex) =>
      context.become(onReceive)
      replyTo ! Failure(ex)
      unstashAll()

    case RoomSequenceResponse(rooms) =>
      context.become(onReceive)
      rooms.find(!this.roomAlreadyJoined(_)) match {
        case Some(room) =>
          val roomActor = system actorOf ClientRoomActor(self, serverUri, room.roomId)
          tryJoinRoomAndReply(ClientRoom(roomActor, serverUri, room.roomId), replyTo)
        case None => replyTo ! Failure(new Exception("No rooms to join"))
      }
      unstashAll()


    case RoomResponse(room) =>
      context.become(onReceive)
      val roomActor = system actorOf ClientRoomActor(self, serverUri, room.roomId)
      tryJoinRoomAndReply(ClientRoom(roomActor, serverUri, room.roomId), replyTo)
      unstashAll()

    case _ => stash


  }


  /**
   * Try to join a given room.
   * If the given room was correctly join adds that room to the set of join rooms and reply success
   * else reply Failure
   */
  private def tryJoinRoomAndReply(clientRoom: ClientRoom, replyTo: ActorRef) =
    clientRoom.join() onComplete {
      case Success(_) =>
        this.joinedRooms = this.joinedRooms + clientRoom
        replyTo ! Success(clientRoom)
      case Failure(ex) => replyTo ! Failure(ex)
    }

  private def roomAlreadyJoined(room: Room): Boolean = this.joinedRooms.map(_.roomId).contains(room.roomId)
}