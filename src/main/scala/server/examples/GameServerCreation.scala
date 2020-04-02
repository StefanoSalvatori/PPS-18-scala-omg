package server.examples

import server.GameServer
import server.room.ServerRoom

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.io.StdIn
import scala.util.{Failure, Success}

object GameServerCreation extends App {

  implicit val executor: ExecutionContextExecutor = ExecutionContext.global

  val HOST = "localhost"
  val PORT = 8080

  val gameServer = GameServer(HOST, PORT)
  gameServer onStart {
    println("GAMESERVER STARTED")
  }
  gameServer onShutdown {
    println("GAMESERVER IS DOWN :-(")
  }

  gameServer.start() onComplete {
    case Success(_) =>
      println("press any key to shutdown...")
      StdIn.readLine()
      gameServer.shutdown()
    case Failure(exception) => println(s"Startup failed: $exception")

  }

  gameServer defineRoom ("test_room", id => ServerRoom(id))


}
