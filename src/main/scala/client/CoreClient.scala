package client

import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.Source
import client.room.ClientRoom.ClientRoom
import common.CommonRoom.{Room, RoomJsonSupport, RoomType}
import common.Routes

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}


sealed trait CoreClient extends BasicActor

object CoreClient {

  import akka.actor.Props

  def apply(serverUri: String): Props = Props(classOf[CoreClientImpl], serverUri)
}

class CoreClientImpl(private val serverUri: String) extends CoreClient with RoomJsonSupport {

  private val httpClient = context.system actorOf HttpClient(serverUri, self)
  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()
  private var joinedRooms: Set[ClientRoom] = Set()

  import MessageDictionary._

  val onReceive: PartialFunction[Any, Unit] = {

    case msg@CreatePublicRoom(roomType, _) =>
      val resTo = sender
      val f: Future[HttpResponse] = Http() singleRequest HttpRequest(
        method = HttpMethods.POST,
        uri = serverUri + "/" + Routes.roomsByType(roomType)
      )

      f onComplete (response => {
        if (response.isSuccess) {
          val res: HttpResponse = response.get
          val unmarshalled = Unmarshal(res).to[Source[ClientRoom, NotUsed]]
          val source = Source futureSource unmarshalled
          source.runFold(Set[ClientRoom]())(_ + _) onComplete { res =>
            if (res.isSuccess) {
              joinedRooms += res.get.head
              resTo ! res.get.head
            } else {
              logger debug s"Failed to parse server response $response"
            }
          }
        } else {
          logger debug s"Failed to receive server response $response"
        }
      })

    case GetAvailableRooms(roomType) =>
      val resTo = sender

      val getRoomsByTypeHttpRequest = HttpRequest(
        method = HttpMethods.GET,
        uri = serverUri + "/" + Routes.roomsByType(roomType)
      )
      logger debug s"Request-> $getRoomsByTypeHttpRequest"
      val f: Future[HttpResponse] = Http() singleRequest getRoomsByTypeHttpRequest


      f onComplete {
        case Success(response) =>
          logger debug s"Response -> $response"
          val unmarshalled: Future[Seq[ClientRoom]] = Unmarshal(response).to[Seq[ClientRoom]]
          resTo ! Await.result(unmarshalled, Duration(5, TimeUnit.SECONDS))

        case Failure(exception) => logger debug s"Failed to get rooms by type"
      }

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