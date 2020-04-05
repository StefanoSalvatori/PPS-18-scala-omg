package client.room

import akka.{Done, NotUsed}
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest, WebSocketUpgradeResponse}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import common.actors.ApplicationActorSystem._

import scala.concurrent.{ExecutionContext, Future}

trait WebSocket[T] {

  val socketUri: String

  def openSocket(): Future[Unit]

  def sendMessage(msg: T): Unit

  def onMessageReceived(callback: T => Unit)

  //TODO: implement
  //def closeSocket(): Unit

}

object WebSocket {
  def apply(receiver: ActorRef, socketUri: String): WebSocket[String] = new BasicWebSocket(receiver, socketUri)
}

/**
 * Handle web socket connection flow
 */
class BasicWebSocket(val receiver: ActorRef, val socketUri: String) extends WebSocket[String] {

  private implicit val executor: ExecutionContext = ExecutionContext.global
  private val BuffSize = 200 //TODO: test best value

  protected var messageReceivedCallback: String => Unit = x => {}

  //incoming
  protected val sink: Sink[Message, NotUsed] =
    Sink.actorRef(receiver, PartialFunction.empty, PartialFunction.empty)


  //outgoing
  protected val (sourceRef, publisher) =
    Source.actorRef(
      PartialFunction.empty, PartialFunction.empty, BuffSize, OverflowStrategy.dropTail)
      .map(TextMessage.Strict)
      .toMat(Sink.asPublisher(false))(Keep.both).run()


  override def openSocket(): Future[Unit] = {
    val flow = Http() webSocketClientFlow (WebSocketRequest(socketUri))
    val ((_, upgradeResponse), _) = Source.fromPublisher(publisher)
      .viaMat(flow)(Keep.both)
      .toMat(sink)(Keep.both)
      .run()

    upgradeResponse map checkOpenSuccess
  }

  override def sendMessage(msg: String): Unit = this.sourceRef ! msg

  override def onMessageReceived(callback: String => Unit): Unit = messageReceivedCallback = callback

  private def checkOpenSuccess(upgradeResponse: WebSocketUpgradeResponse) = {
    // status code 101 (Switching Protocols) indicates that server support WebSockets
    if (upgradeResponse.response.status != StatusCodes.SwitchingProtocols) {
      throw new Exception(s"Connection failed: ${upgradeResponse.response.status}")
    }
  }

}











