package client

import akka.actor.{Actor, ActorSystem}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.util.ByteString
import akka.pattern.pipe

sealed trait ClientActor extends Actor

object ClientActor {
  import akka.actor.Props
  def apply(serverUri: String): Props = Props(classOf[ClientActorImpl], serverUri)
}

class ClientActorImpl(private val serverUri: String) extends ClientActor {

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