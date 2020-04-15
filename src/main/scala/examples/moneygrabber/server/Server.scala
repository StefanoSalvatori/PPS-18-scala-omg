package examples.moneygrabber.server

import examples.moneygrabber.server.rooms.MatchRoom
import server.GameServer

object Server extends App {
  private val Host = "192.168.1.4"
  private val Port = 8080
  val server: GameServer = GameServer(Host, Port)
  server onStart {
    println(s"Server listening at $Host:$Port")
  }
  server.defineRoom("game", MatchRoom)
  server.start()
}
