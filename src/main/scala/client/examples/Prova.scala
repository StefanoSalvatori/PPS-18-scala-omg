package client.examples

import server.matchmaking.Matchmaker

object Prova extends App {

  private val m = Matchmaker defaultMatchmaker Map(1 -> 1, 2 -> 2, 3 -> 1)

  var clients = (0 until 10).map(i => server.room.Client.placeholder(s"$i") -> 0).toMap

  println(m createFairGroup clients)
}
