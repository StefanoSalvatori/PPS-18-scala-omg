package scalaomg.examples.moneygrabber.server

import scalaomg.examples.moneygrabber.server.rooms.MatchRoom
import scalaomg.server.core.GameServer
object Server extends App {
  private val Host = if (args.length > 0) args(0) else ServerInfo.DefaultHost
  private val Port = if (args.length > 1) args(1).toInt else ServerInfo.DefaultPort
  private val server: GameServer = GameServer(Host, Port)
  server onStart {
    println(s"Server listening at $Host:$Port")
  }
  server.defineRoom("game", MatchRoom)
  server.start()
}
