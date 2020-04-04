package client

import akka.actor.{ActorRef, Stash}
import akka.pattern.ask
import akka.util.Timeout
import client.room.ClientRoom
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
  private val httpClient = context.system actorOf HttpClient(serverUri, self)
  private var joinedRooms: Set[ClientRoom] = Set()

  override def receive: Receive = onReceive orElse fallbackReceive

  def waitRoomsToJoin: Receive = onWaitRoomsToJoin orElse fallbackReceive


  val onReceive: Receive = {

    case RoomSequenceResponse(rooms) => sender ! Success(rooms)

    case FailResponse(ex) => sender ! Failure(ex)

    case CreatePublicRoom(roomType, roomOptions) =>
      context.become(this.onWaitRoomsToJoin)
      this.httpClient forward HttpPostRoom(roomType, roomOptions)

    case GetAvailableRooms(roomType, roomOptions) =>
      this.httpClient forward HttpGetRooms(roomType, roomOptions)

    case Join(roomType, roomOptions) =>
      this.httpClient forward HttpGetRooms(roomType, roomOptions)
      context.become(this.onWaitRoomsToJoin)

    case JoinById(roomId) =>
      val replyTo = sender
      this.joinedRooms.find(_.roomId == roomId) match {
        case Some(_) => replyTo ! Failure(new Exception("Room already joined"))
        case None =>
          context.become(this.onWaitRoomsToJoin)
          self forward RoomResponse(ClientRoom(serverUri, roomId))
      }

    case GetJoinedRooms =>
      sender ! JoinedRooms(this.joinedRooms)
  }


  /**
   * Behavior to handle response from HttpClientActor
   */
  val onWaitRoomsToJoin: Receive = {

    case CreatePublicRoom(_, _) => stash

    case FailResponse(ex) =>
      sender ! Failure(ex)
      unstashAll()
      context.become(onReceive)

    case RoomSequenceResponse(rooms) =>
      val replyTo = sender
      rooms.find(!this.roomAlreadyJoined(_)) match {
        case Some(room) => tryJoinRoomAndReply(ClientRoom(serverUri, room.roomId), sender)
        case None => replyTo ! Failure(new Exception("No rooms to join"))
      }
      unstashAll()
      context.become(onReceive)


    case RoomResponse(room) =>
      tryJoinRoomAndReply(ClientRoom(serverUri, room.roomId), sender)
      unstashAll()
      context.become(onReceive)


  }


  /**
   * Try to join a given room.
   * If the given room was correctly join adds that room to the set of join rooms and reply success
   * else reply Failure
   */
  private def tryJoinRoomAndReply(clientRoom: ClientRoom, replyTo: ActorRef): Unit =
    clientRoom.join() onComplete {
      case Success(_) =>
        this.joinedRooms = this.joinedRooms + clientRoom
        replyTo ! Success(clientRoom)
      case Failure(ex) => replyTo ! Failure(ex)
    }

  private def roomAlreadyJoined(room: Room): Boolean = this.joinedRooms.map(_.roomId).contains(room.roomId)
}