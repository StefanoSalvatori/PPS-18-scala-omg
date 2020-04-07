package server.examples

import server.GameServer
import server.room.ServerRoom

import scala.concurrent.ExecutionContext
import scala.io.StdIn
import scala.util.{Failure, Success}

object GameServerCreation extends App {

  implicit val executor: ExecutionContext = ExecutionContext.global
  val HOST = "localhost"
  val PORT = 8080

  val gameServer = GameServer(HOST, PORT)
  gameServer onStart {
    println("GAMESERVER STARTED")
  }
  gameServer onStop {
    println("GAMESERVER IS DOWN :-(")
  }

  gameServer.start() onComplete {
    case Success(_) =>
      println("press any key to shutdown...")
      StdIn.readLine()
      gameServer.stop()
      StdIn.readLine("press any key to start...")
      gameServer.start()
      StdIn.readLine("starting...")

    case Failure(exception) => println(s"Startup failed: $exception")

  }

  gameServer defineRoom("test_room", id => ServerRoom(id))
}
