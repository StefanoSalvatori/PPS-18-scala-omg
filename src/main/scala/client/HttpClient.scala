package client

import akka.actor.{Actor, ActorSystem, Props}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.pattern.pipe
import akka.util.ByteString

sealed trait HttpClient extends Actor

object HttpClient {
  def apply(serverUri: String): Props = Props(classOf[HttpClientImpl], serverUri)
}

class HttpClientImpl(private val serverUri: String) extends HttpClient {

  implicit val system: ActorSystem = context.system
  import scala.concurrent.ExecutionContext
  implicit val executionContext: ExecutionContext = system.dispatcher

  import akka.http.scaladsl.Http
  private val http = Http()

  import MessageDictionary._
  override def receive: Receive = {

    case CreatePublicRoom =>
      http singleRequest HttpRequest(
        method = HttpMethods.POST,
        uri = serverUri + Routes.publicRooms
      ) pipeTo self

    case HttpResponse(StatusCodes.OK, headers, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        println("Response body: " + body.utf8String)
      }

    case response@HttpResponse(code, _, _, _) =>
      println("Request failed, response code: " + code)
      response.discardEntityBytes()

    case _ =>
      print("Ignoring unknown message: " + _)
      sender ! UnknownMessageReply
  }
}
