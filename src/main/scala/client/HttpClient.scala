package client

import akka.NotUsed
import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling._
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.common.JsonEntityStreamingSupport
import akka.stream.scaladsl.Source
import client.room.ClientRoom
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
  }



  override def receive: Receive = onReceive orElse fallbackReceive
}
