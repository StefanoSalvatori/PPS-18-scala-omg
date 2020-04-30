package server.examples

import server.GameServer

import scala.concurrent.ExecutionContext

object GameServerCreation extends App {
  implicit private val executor: ExecutionContext = ExecutionContext.global
  private val Host = "localhost"
  private val Port = 8080
  private val gameServer = GameServer(Host, Port)
  gameServer onStart {
    println("GAMESERVER STARTED")
  }
  gameServer.start()
  Thread.sleep(5000)
  gameServer.terminate()
}
