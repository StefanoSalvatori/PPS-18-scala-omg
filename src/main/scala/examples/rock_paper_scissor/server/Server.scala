package examples.rock_paper_scissor.server

import server.GameServer

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}


object Server extends App {

  implicit private val executor: ExecutionContext = ExecutionContext.global
  private val DefaultPort = 8080

  private val host = if (args.length > 0) args(0) else "localhost"
  private val port = if (args.length > 1) args(1).toInt else DefaultPort

  private val gameServer = GameServer(host, port)

  //define room types to host matches
  gameServer defineRoom("classic", () => new ClassicMatchRoom())
  gameServer defineRoom("advanced", () => new AdvancedMatchRoom())

  gameServer.start() onComplete {
    case Success(_) => println(s"Server started at $host:$port")
    case Failure(exception) => println(s"Startup failed: $exception")
  }



}
