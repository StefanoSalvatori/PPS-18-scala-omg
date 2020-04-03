package client

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.unmarshalling.Unmarshal
import client.room.ClientRoom
import common.CommonRoom.Room
import common.{HttpRequests, RoomJsonSupport}

import scala.concurrent.Future

sealed trait CoreClient extends BasicActor

object CoreClient {

  import akka.actor.Props

  def apply(serverUri: String): Props = Props(classOf[CoreClientImpl], serverUri)
}

class CoreClientImpl(private val serverUri: String) extends CoreClient with RoomJsonSupport {

  private val httpClient = context.system actorOf HttpClient(serverUri, self)
  private var joinedRooms: Set[ClientRoom] = Set()

  import MessageDictionary._

  val onReceive: Receive = {
    case CreatePublicRoom(roomType, _) =>
      val resTo = sender
      val createRoomFuture: Future[HttpResponse] = Http() singleRequest
        HttpRequests.postRoom(serverUri)(roomType)

      (for {
        response <- createRoomFuture
        room <- Unmarshal(response).to[Room]
      } yield ClientRoom(serverUri, room.roomId)) foreach { room =>
        joinedRooms = joinedRooms + room
        resTo ! room
      }

    case GetAvailableRooms(roomType) =>
      val resTo = sender

      val getRoomsFuture: Future[HttpResponse] = Http() singleRequest
        HttpRequests.getRoomsByType(serverUri)(roomType)

      (for {
        response <- getRoomsFuture
        rooms <- Unmarshal(response).to[Seq[Room]]
      } yield rooms) foreach {
        resTo ! _
      }

    case Join(roomType, _) =>
      val replyTo = sender

      val getRoomsFuture: Future[HttpResponse] = Http() singleRequest
        HttpRequests.getRoomsByType(serverUri)(roomType)

      (for {
        response <- getRoomsFuture
        rooms <- Unmarshal(response).to[Seq[Room]]
        clientRoom =  ClientRoom(serverUri, rooms.head.roomId)
        _ <- clientRoom.join()
      } yield clientRoom) foreach { replyTo ! _ }

    case JoinOrCreate(roomType, _) => //TODO: implement

    case JoinById(roomId)=> //TODO: implement


    case NewJoinedRoom(room)  =>
      if (joinedRooms map (_ roomId) contains room.roomId) {
        logger debug s"Room ${room.roomId} was already joined!"
      } else {
        joinedRooms += room
        logger debug s"New joined room ${room.roomId}"
      }
      logger debug s"Current joined rooms: $joinedRooms"

    case GetJoinedRooms =>
      sender ! JoinedRooms(joinedRooms)
  }

  override def receive: Receive = onReceive orElse fallbackReceive
}