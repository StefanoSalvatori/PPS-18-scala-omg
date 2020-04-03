package client

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.unmarshalling._
import client.room.ClientRoom
import common.CommonRoom.Room
import common.{HttpRequests, RoomJsonSupport}

import scala.concurrent.Future
import scala.util.{Failure, Success}

sealed trait HttpClient extends BasicActor

object HttpClient {
  def apply(serverUri: String, coreClient: ActorRef): Props = Props(classOf[HttpClientImpl], serverUri, coreClient)
}

class HttpClientImpl(private val serverUri: String, private val coreClient: ActorRef) extends HttpClient with RoomJsonSupport {

  import akka.http.scaladsl.Http

  private val http = Http()

  import MessageDictionary._

  private val onReceive: PartialFunction[Any, Unit] = {

    case HttpPostRoom(roomType, _) =>
      val replyTo = sender
      val createRoomFuture: Future[HttpResponse] = http singleRequest HttpRequests.postRoom(serverUri)(roomType)
      (for {
        response <- createRoomFuture
        room <- Unmarshal(response).to[Room]
      } yield room) onComplete {
        case Success(value) => coreClient.tell(RoomResponse(value), replyTo)
        case Failure(ex) => coreClient.tell(FailResponse(ex), replyTo)
      }

    case HttpGetRooms(roomType, _) =>
      val replyTo = sender
      val getRoomsFuture: Future[HttpResponse] = http singleRequest HttpRequests.getRoomsByType(serverUri)(roomType)
      (for {
        response <- getRoomsFuture
        rooms <- Unmarshal(response).to[Seq[Room]]
      } yield rooms.map(r => ClientRoom(serverUri, r.roomId))) onComplete {
        case Success(value) => coreClient.tell(RoomSequenceResponse(value), replyTo)
        case Failure(ex) => coreClient.tell(FailResponse(ex), replyTo)
      }
  }


  override def receive: Receive = onReceive orElse fallbackReceive
}
