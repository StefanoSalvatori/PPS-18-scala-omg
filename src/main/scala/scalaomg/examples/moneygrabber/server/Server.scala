package scalaomg.examples.moneygrabber.server

import scalaomg.examples.moneygrabber.server.rooms.MatchRoom
import scalaomg.server.core.GameServer

object Server extends App {
  val Host = ServerConfig.ServerHost
  val Port = ServerConfig.ServerPort
  private val server: GameServer = GameServer(Host, Port)
  server onStart {
    println(s"Server listening at $Host:$Port")
  }
  server.defineRoom("game", MatchRoom)
  server.start()
}
