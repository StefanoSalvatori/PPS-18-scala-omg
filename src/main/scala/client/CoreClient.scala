package client

import akka.http.scaladsl.Http
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.unmarshalling.Unmarshal
import client.room.ClientRoom.ClientRoom
import common.{HttpRequests, RoomJsonSupport}

import scala.concurrent.Future
import scala.util.{Failure, Success}


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
        room <- Unmarshal(response).to[ClientRoom]
      } yield room) onComplete {
        case Success(room) =>
          joinedRooms = joinedRooms + room
          resTo ! room
        case Failure(exception) => Future.failed(exception)
      }

    case GetAvailableRooms(roomType) =>
      val resTo = sender

      val getRoomsFuture: Future[HttpResponse] = Http() singleRequest
        HttpRequests.getRoomsByType(serverUri)(roomType)

      (for {
        response <- getRoomsFuture
        rooms <- Unmarshal(response).to[Seq[ClientRoom]]
      } yield rooms) onComplete {
        case Success(rooms) => resTo ! rooms
        case Failure(exception) => Future.failed(exception)
      }

    case Join(roomType, _) =>
      val resTo = sender

      val getRoomsFuture: Future[HttpResponse] = Http() singleRequest
        HttpRequests.getRoomsByType(serverUri)(roomType)

      (for {
        response <- getRoomsFuture
        rooms <- Unmarshal(response).to[Seq[ClientRoom]]
        joinedRoom <-  rooms.head.join()
      } yield joinedRoom) onComplete {
        case Success(r) => resTo ! r
        case Failure(ex) => Future.failed(ex)
      }



    case JoinOrCreate(roomType, _) =>

    case JoinById(roomId) =>

    case NewJoinedRoom(room) =>
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