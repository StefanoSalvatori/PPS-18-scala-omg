package client.room

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{Flow, Sink}

import scala.concurrent.{ExecutionContext, Future, Promise}

/**
 * Handle a web socket connection with a server room
 */
trait RoomSocket extends WebSocket[String] {

  /**
   * Send a join message and wait for a response from the server
   * @return a future that succeed if the room can be joined
   */
  def sendJoin(): Future[Unit]

  /**
   * Send a leave message to the server room
   */
  def sendLeave(): Unit

}

object RoomSocket {
  def apply(socketUri: String)(implicit actorSystem: ActorSystem): RoomSocket = new RoomSocketImpl(socketUri)
}

/**
 * Handle web socket connection
 */
class RoomSocketImpl(override val socketUri: String)(private implicit val actorSystem: ActorSystem)
  extends BasicWebSocket(socketUri) with RoomSocket {

  private implicit val executor: ExecutionContext = actorSystem.dispatcher
  private var joinFuture = Promise.successful()

  //incoming
  override protected val sink  = Flow[Message]
    .map {
      //TODO: this should match some protocol messages
      case TextMessage.Strict("join") => joinFuture.success()
      case TextMessage.Strict("joinFail") => joinFuture.failure(new Exception("Unable to join"))


      case TextMessage.Strict(value) => messageReceivedCallback(value)
    }.to(Sink.onComplete(_ => println("complete")))



  override def sendJoin(): Future[Unit] = {
    //create a promise that will complete on server response
    joinFuture = Promise[Unit]()
    this.sendMessage("join")
    joinFuture.future

  }
  override def sendLeave(): Unit = this.sendMessage("leave") //TODO: close the socket


}
