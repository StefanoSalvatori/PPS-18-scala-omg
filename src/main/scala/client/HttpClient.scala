package client

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.pattern.pipe
import akka.util.ByteString
import common.Routes

sealed trait HttpClient extends BasicActor

object HttpClient {
  def apply(serverUri: String, coreClient: ActorRef): Props = Props(classOf[HttpClientImpl], serverUri, coreClient)
}

class HttpClientImpl(private val serverUri: String, private val coreClient: ActorRef) extends HttpClient {

  import akka.http.scaladsl.Http
  private val http = Http()

  import MessageDictionary._
  private val onReceive: PartialFunction[Any, Unit] = {

    case CreatePublicRoom =>
      http singleRequest HttpRequest(
        method = HttpMethods.POST,
        uri = serverUri + Routes.publicRooms
      ) pipeTo self

    case HttpResponse(StatusCodes.OK, _, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        println("Response body: " + body.utf8String)
      }

    case response @ HttpResponse(code, _, _, _) =>
      println("Request failed, response code: " + code)
      response.discardEntityBytes()
  }

  override def receive: Receive = onReceive orElse fallbackReceive
}
