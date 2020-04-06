package client

import akka.NotUsed
import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.model.ws.{InvalidUpgradeResponse, Message, TextMessage, ValidUpgrade, WebSocketRequest}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling._
import akka.pattern.pipe
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Sink, Source}
import client.utils.MessageDictionary._
import common.SharedRoom.Room
import common.{HttpRequests, RoomJsonSupport, Routes}

import scala.concurrent.Future
import scala.util.{Failure, Success}

sealed trait HttpClient extends BasicActor

object HttpClient {
  def apply(serverUri: String): Props = Props(classOf[HttpClientImpl], serverUri)
}

class HttpClientImpl(private val httpServerUri: String) extends HttpClient with RoomJsonSupport {

  import akka.http.scaladsl.Http

  private val http = Http()

  override def receive: Receive = onReceive orElse fallbackReceive

  def waitSocketResponse(replyTo: ActorRef, outRef: ActorRef): Receive =
    onWaitSocketResponse(replyTo, outRef) orElse fallbackReceive

  private val onReceive: Receive = {

    case HttpPostRoom(roomType, roomProperties) =>
      val replyTo = sender
      val createRoomFuture: Future[HttpResponse] =
        http singleRequest HttpRequests.postRoom(httpServerUri)(roomType, roomProperties)
      (for {
        response <- createRoomFuture
        room <- Unmarshal(response).to[Room]
      } yield room) onComplete {
        case Success(value) => replyTo ! HttpRoomResponse(value)
        case Failure(ex) => replyTo ! FailResponse(ex)
      }

    case HttpGetRooms(roomType, filterOptions) =>
      val replyTo = sender
      val getRoomsFuture: Future[HttpResponse] =
        http singleRequest HttpRequests.getRoomsByType(httpServerUri)(roomType, filterOptions)
      (for {
        response <- getRoomsFuture
        rooms <- Unmarshal(response).to[Seq[Room]]
      } yield rooms) onComplete {
        case Success(value) => replyTo ! HttpRoomSequenceResponse(value)
        case Failure(ex) => replyTo ! FailResponse(ex)
      }

    case HttpSocketRequest(roomId) =>
      val sink: Sink[Message, NotUsed] =
        Sink.actorRef(sender, PartialFunction.empty, PartialFunction.empty)

      val (sourceRef, publisher) =
        Source.actorRef(
          PartialFunction.empty, PartialFunction.empty, Int.MaxValue, OverflowStrategy.dropTail)
          .toMat(Sink.asPublisher(false))(Keep.both).run()

      val wsSocketUri = Routes.wsUri(this.httpServerUri) + "/" + Routes.webSocketConnection(roomId)
      val flow = Http() webSocketClientFlow (WebSocketRequest(wsSocketUri))
      val ((_, upgradeResponse), _) = Source.fromPublisher(publisher)
        .viaMat(flow)(Keep.both)
        .toMat(sink)(Keep.both)
        .run()

      upgradeResponse pipeTo self
      context.become(waitSocketResponse(sender, sourceRef))
  }


  def onWaitSocketResponse(replyTo: ActorRef, outRef: ActorRef): Receive = {
    case ValidUpgrade(_, _) =>
      replyTo ! HttpSocketSuccess(outRef)
      context.unbecome()

    case InvalidUpgradeResponse(_, cause) =>
      replyTo ! HttpSocketFail(cause)
      context.unbecome()

  }


}


