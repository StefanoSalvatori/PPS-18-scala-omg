package client.room

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{Flow, Sink}
import common.actors.ApplicationActorSystem._

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
  def apply(receiver: ActorRef, socketUri: String): RoomSocket = new RoomSocketImpl(receiver, socketUri)
}

/**
 * Handle web socket connection
 */
class RoomSocketImpl(override val receiver: ActorRef,
                     override val socketUri: String) extends BasicWebSocket(receiver, socketUri) with RoomSocket {

  private implicit val executor: ExecutionContext = ExecutionContext.global
  private var joinFuture = Promise.successful()



  override def sendJoin(): Future[Unit] = {
    //create a promise that will complete on server response
    joinFuture = Promise[Unit]()
    this.sendMessage("join")
    joinFuture.future

  }
  override def sendLeave(): Unit = this.sendMessage("leave") //TODO: close the socket


}
