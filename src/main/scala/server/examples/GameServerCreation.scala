package server.examples

import server.core.GameServer

import scala.concurrent.ExecutionContext
import scala.io.StdIn
import scala.util.{Failure, Success}

object GameServerCreation extends App {

  implicit private val executor: ExecutionContext = ExecutionContext.global
  private val Host = "localhost"
  private val Port = 8080

  private val gameServer = GameServer(Host, Port)
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

  import server.examples.rooms.ExampleRooms._
  gameServer defineRoom("test_room", MyRoom)
}
