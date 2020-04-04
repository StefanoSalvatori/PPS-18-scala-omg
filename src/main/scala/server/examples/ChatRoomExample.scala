package server.examples

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import common.actors.ApplicationActorSystem
import server.GameServer
import server.room._

import scala.concurrent.Await
import scala.io.StdIn

class ChatRoom(override val roomId: String) extends ServerRoom {
  override def onCreate(): Unit = println("Room Created")

  override def onClose(): Unit = println("Room Closed")

  override def onJoin(client: Client): Unit = this.broadcast(s"${client.id} Connected")

  override def onLeave(client: Client): Unit = this.broadcast(s"${client.id} Leaved")

  override def onMessageReceived[M](client: Client, message: M): Unit = this.broadcast(message)
}


object ChatRoomExample extends App {
  implicit val actorSystem: ActorSystem = ActorSystem()
  val HOST: String = "localhost"
  val PORT: Int = 8080
  val ESCAPE_TEXT = "quit"
  val ROOM_PATH = "chat"
  val gameServer: GameServer = GameServer(HOST, PORT)
  gameServer.defineRoom(ROOM_PATH, id => new ChatRoom(id))

  import scala.concurrent.duration._
  Await.ready(gameServer.start(), 10 seconds)
  while (StdIn.readLine(s"press any key to create chatRoom; type '$ESCAPE_TEXT' to exit \n") != ESCAPE_TEXT) {
    Http().singleRequest(HttpRequest(HttpMethods.POST, uri = s"http://$HOST:$PORT/rooms/$ROOM_PATH"))
  }
  Await.ready(gameServer.shutdown(), 10 seconds)
  // Await.ready(terminateActorSystem(), 10 seconds)
  System.exit(0)

}
