package client.room

import akka.actor.ActorRef
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import common.CommonRoom.RoomId
import common.HttpRequests
import common.actors.ApplicationActorSystem

import scala.concurrent.{Future, Promise}

trait SocketHandler {

  def openSocket(): Future[Unit]

  def join(): Future[Unit]

  def leave(): Unit

  def sendMessage(msg: String): Unit

  def onMessageReceived(callback: String => Unit)
}

object SocketHandler {
  def apply(serverUri: String, roomId: RoomId): SocketHandler = SocketHandlerImpl(serverUri, roomId)
}


/**
 * Handle web socket connection
 */
import common.actors.ApplicationActorSystem._
private case class SocketHandlerImpl(serverUri: String, roomId: RoomId) extends SocketHandler  {

  private val BuffSize = 200 //TODO: test best value
  private var joinFuture = Promise.successful()
  private var messageReceivedCallback: String => Unit = x => {}

  //incoming
  val sink = Flow[Message]
    .map {
      //TODO: this should match some protocol messages
      case TextMessage.Strict("join") => joinFuture.success()
      case TextMessage.Strict("joinFail") => joinFuture.failure(new Exception("Unable to join"))
      case TextMessage.Strict(value) => messageReceivedCallback(value)
    }.to(Sink.onComplete(_ => println("complete")))

  //outgoing
  val (serverRoomRef, publisher) = Source.actorRef(
    PartialFunction.empty,
    PartialFunction.empty,
    bufferSize = BuffSize,
    overflowStrategy = OverflowStrategy.dropTail)
    .map(TextMessage.Strict)
    .toMat(Sink.asPublisher(false))(Keep.both).run()

  // in ~> out
  val flow = Flow.fromSinkAndSource(sink, Source.fromPublisher(publisher))


  override def openSocket(): Future[Unit] = {
    val flow = Http() webSocketClientFlow (HttpRequests.connectToRoom(serverUri)(roomId))

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


  override def join(): Future[Unit] = {
    //create a promise that will complete on server response
    joinFuture = Promise[Unit]()
    this.sendMessage("join")
    joinFuture.future

  }

  override def leave(): Unit = this.sendMessage("leave") //TODO: close the socket


  override def sendMessage(msg: String): Unit = this.serverRoomRef ! msg


  override def onMessageReceived(callback: String => Unit): Unit = messageReceivedCallback = callback

}





