package scalaomg.examples.roll_the_dice.server

import scalaomg.examples.roll_the_dice.server.room.{CustomMatchmaker, MatchRoom}
import scalaomg.server.core.GameServer

object Server extends App {

  import scala.concurrent.ExecutionContext
  implicit private val executor: ExecutionContext = ExecutionContext.global

  import scalaomg.examples.roll_the_dice.common.ServerConfig._
  private val server = GameServer(host, port)
  server onStart {
    println(s"Server listening at $host:$port")
  }

  server.start()

  val roomName = "matchRoom"
  server.defineRoomWithMatchmaking(roomName, MatchRoom, CustomMatchmaker())
}
