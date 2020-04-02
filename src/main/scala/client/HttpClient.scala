package client

import akka.NotUsed
import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling._
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.common.JsonEntityStreamingSupport
import akka.stream.scaladsl.Source
import client.room.ClientRoom.ClientRoom
import common.{RoomJsonSupport, Routes}

import scala.concurrent.Future

sealed trait HttpClient extends BasicActor

object HttpClient {
  def apply(serverUri: String, coreClient: ActorRef): Props = Props(classOf[HttpClientImpl], serverUri, coreClient)
}

class HttpClientImpl(private val serverUri: String, private val coreClient: ActorRef) extends HttpClient with RoomJsonSupport {

  import akka.http.scaladsl.Http
  private val http = Http()

  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

  import MessageDictionary._
  private val onReceive: PartialFunction[Any, Unit] = {

    case CreatePublicRoom(roomType, _) =>
      val f: Future[HttpResponse] = http singleRequest HttpRequest(
        method = HttpMethods.POST,
        uri = serverUri + "/" + Routes.roomsByType(roomType)
      )

      f onComplete (response => {
        if (response.isSuccess) {
          val res: HttpResponse = response.get
          val unmarshalled: Future[Source[ClientRoom, NotUsed]] = Unmarshal(res).to[Source[ClientRoom, NotUsed]]
          val source = Source futureSource unmarshalled
          source.runFold(Set[ClientRoom]())(_ + _) onComplete { res =>
            if (res.isSuccess) {
              coreClient ! NewJoinedRoom(res.get.head)
            } else {
              logger debug s"Failed to parse server response $response"
            }
          }
        } else {
          logger debug s"Failed to receive server response $response"
        }
      })
  }



  override def receive: Receive = onReceive orElse fallbackReceive
}
