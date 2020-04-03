package client.room

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import org.reactivestreams.Publisher

import scala.concurrent.{ExecutionContext, Future}

trait SocketHandler[T] {

  val socketUri: String

  def openSocket(): Future[Unit]

  def sendMessage(msg: T): Unit

  def onMessageReceived(callback: T => Unit)


  //TODO: implement
  //def closeSocket(): Unit

}

object SocketHandler {
  def apply(socketUri: String)(implicit actorSystem: ActorSystem): SocketHandler[String] = new BasicSocketHandler(socketUri)
}

/**
 * Handle web socket connection flow
 */
 class BasicSocketHandler(val socketUri: String)
                         (private implicit val actorSystem: ActorSystem) extends SocketHandler[String] {

  private implicit val executor: ExecutionContext = actorSystem.dispatcher

  private val BuffSize = 200 //TODO: test best value
  private var messageReceivedCallback: String => Unit = x => {}

  //incoming
  protected val sink: Sink[Message, NotUsed] = Flow[Message].map {
      case TextMessage.Strict(value) => messageReceivedCallback(value)
    }.to(Sink.onComplete(_ => println("complete")))

  //outgoing
  protected val (sourceRef, publisher) = Source.actorRef(
    PartialFunction.empty,
    PartialFunction.empty,
    bufferSize = BuffSize,
    overflowStrategy = OverflowStrategy.dropTail)
    .map(TextMessage.Strict)
    .toMat(Sink.asPublisher(false))(Keep.both).run()


  override def openSocket(): Future[Unit] = {
    val flow = Http() webSocketClientFlow (WebSocketRequest(socketUri))

    //materialize flow
    (Source.fromPublisher(publisher)
      .viaMat(flow)(Keep.both)
      .toMat(sink)(Keep.both)
      .run())._1._2 map {
      upgrade =>
        // status code 101 (Switching Protocols) indicates that server support WebSockets
        if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
          //if OK complete future with Done else fail
          Future.successful()
        } else {
          Future.failed(new RuntimeException(s"Connection failed: ${upgrade.response.status}"))
        }
    }
  }

  override def sendMessage(msg: String): Unit = this.sourceRef ! msg


  override def onMessageReceived(callback: String => Unit): Unit = messageReceivedCallback = callback

}











