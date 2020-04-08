package examples.rock_paper_scissor.server

import server.GameServer
import server.room.{Client, ServerRoom}

import scala.concurrent.ExecutionContext
import scala.io.StdIn
import scala.util.{Failure, Success}



object Server extends App {

  implicit val executor: ExecutionContext = ExecutionContext.global
  val Host = "localhost"
  val Port = 8080

  val gameServer = GameServer(Host, Port)

  //define a room type to host matches
  gameServer defineRoom("match", id => new MatchRoom(id))

  gameServer.start() onComplete {
    case Success(_) => println(s"Server started at $Host:$Port")
    case Failure(exception) => println(s"Startup failed: $exception")

  }



}
