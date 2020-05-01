package scalaomg.examples.roll_the_dice.server

import scalaomg.examples.roll_the_dice.common.ServerInfo
import scalaomg.examples.roll_the_dice.server.room.{CustomMatchmaker, MatchRoom}
import scalaomg.server.core.GameServer

object Server extends App {
  import scala.concurrent.ExecutionContext
  implicit private val executor: ExecutionContext = ExecutionContext.global

  private val host = if (args.length > 0) args(0) else ServerInfo.DefaultHost
  private val port = if (args.length > 1) args(1).toInt else ServerInfo.DefaultPort
  private val server = GameServer(host, port)
  server onStart {
    println(s"Server listening at $host:$port")
  }

  server.start()

  val roomName = "matchRoom"
  server.defineRoomWithMatchmaking(roomName, MatchRoom, CustomMatchmaker())
}
