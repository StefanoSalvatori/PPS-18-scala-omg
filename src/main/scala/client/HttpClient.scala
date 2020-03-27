package client

import akka.NotUsed
import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.pattern.pipe
import common.{Room, RoomJsonSupport, Routes}
import akka.http.scaladsl.unmarshalling._
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.common.JsonEntityStreamingSupport
import akka.stream.scaladsl.Source

import scala.concurrent.Future

sealed trait HttpClient extends BasicActor with RoomJsonSupport

object HttpClient {
  def apply(serverUri: String, coreClient: ActorRef): Props = Props(classOf[HttpClientImpl], serverUri, coreClient)
}

class HttpClientImpl(private val serverUri: String, private val coreClient: ActorRef) extends HttpClient {

  import akka.http.scaladsl.Http
  private val http = Http()

  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

  import MessageDictionary._
  private val onReceive: PartialFunction[Any, Unit] = {

    case CreatePublicRoom =>
      http singleRequest HttpRequest(
        method = HttpMethods.POST,
        uri = serverUri + "/" + Routes.roomsByType("test_room")
      ) pipeTo self

    case response @ HttpResponse(StatusCodes.OK, _, _, _) =>
      val unmarshalled: Future[Source[Room, NotUsed]] = Unmarshal(response).to[Source[Room, NotUsed]]
      val source = Source futureSource unmarshalled
      source.runFold(Set[Room]())(_ + _) onComplete { res =>
        if (res.isSuccess) {
          logger debug s"$res.get"
        } else {
          logger debug s"Failed to parse server response $response"
        }
      }

    case response @ HttpResponse(code, _, _, _) =>
      println("Request failed, response code: " + code)
      response.discardEntityBytes()
  }

  override def receive: Receive = onReceive orElse fallbackReceive
}
