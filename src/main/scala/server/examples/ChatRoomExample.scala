package server.examples

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import server.GameServer
import server.examples.rooms.ChatRoom
import server.room._

import scala.concurrent.Await
import scala.io.StdIn


object ChatRoomExample extends App {
  implicit val actorSystem: ActorSystem = ActorSystem()
  val HOST: String = "localhost"
  val PORT: Int = 8080
  val ESCAPE_TEXT = "quit"
  val ROOM_PATH = "chat"
  val gameServer: GameServer = GameServer(HOST, PORT)
  gameServer.defineRoom(ROOM_PATH, ChatRoom)

  import scala.concurrent.duration._
  Await.ready(gameServer.start(), 10 seconds)
  while (StdIn.readLine(s"press any key to create chatRoom; type '$ESCAPE_TEXT' to exit \n") != ESCAPE_TEXT) {
    Http().singleRequest(HttpRequest(HttpMethods.POST, uri = s"http://$HOST:$PORT/rooms/$ROOM_PATH"))
  }
  Await.ready(gameServer.stop(), 10 seconds)
  gameServer.terminate()


}
