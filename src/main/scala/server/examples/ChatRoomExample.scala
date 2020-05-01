package server.examples

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import server.core.GameServer
import server.examples.rooms.ChatRoom

import scala.concurrent.Await
import scala.io.StdIn

object ChatRoomExample extends App {
  implicit private val actorSystem: ActorSystem = ActorSystem()
  private val Host: String = "localhost"
  private val Port: Int = 8080
  private val EscapeExit = "quit"
  private val RoomPath = "chat"
  private val gameServer: GameServer = GameServer(Host, Port)
  gameServer.defineRoom(RoomPath, ChatRoom)

  import scala.concurrent.duration._
  Await.ready(gameServer.start(), 10 seconds)
  while (StdIn.readLine(s"press any key to create chatRoom; type '$EscapeExit' to exit \n") != EscapeExit) {
    Http().singleRequest(HttpRequest(HttpMethods.POST, uri = s"http://$Host:$Port/rooms/$RoomPath"))
  }
  Await.ready(gameServer.stop(), 10 seconds)
  gameServer.terminate()
}
