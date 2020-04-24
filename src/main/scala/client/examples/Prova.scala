package client.examples

import server.matchmaking.{DefaultMatchMakingStrategy, MatchMakingStrategy}

object Prova extends App {

  private val m = DefaultMatchMakingStrategy(Map(1 -> 1, 2 -> 2, 3 -> 1))

  var clients = Map[server.room.Client, Any]()
  for (i <- 0 until 10) clients = clients + (server.room.Client.empty(s"$i") -> 0)


  println(m.createFairGroupStrategy(clients))
}
