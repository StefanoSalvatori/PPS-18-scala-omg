package client

import common.actors.ApplicationActorSystem
import server.GameServer

import scala.concurrent.Await
import scala.util.{Failure, Success}
import scala.concurrent.duration._
object ClientStub extends App with ApplicationActorSystem{

  private val serverAddress = "localhost"
  private val serverPort = 8080

 /* private val gameServer = GameServer(serverAddress, serverPort)
  gameServer.defineRoom("test_room", RoomStrategy.empty)
  Await.ready(gameServer.start(), 5 seconds)
  println(s"Server started at $serverAddress:$serverPort")*/

  val client = Client(serverAddress, serverPort)
  client createPublicRoom("test_room", "") onComplete {
    case Success(room) => println("Room id ->" + room.roomId)
    case Failure(exception) => println("Fail " + exception)
  }

  //client.shutdown()
}
