package examples.moneygrabber.server.rooms

import examples.moneygrabber.common.Entities.Player
import examples.moneygrabber.common.Model.{Direction, World}
import server.room.{Client, ServerRoom, SynchronizedRoomState}

case class MatchRoom() extends ServerRoom with SynchronizedRoomState[World] {


  private var players: Map[String, Int] = Map.empty
  val worldSize: (Int, Int) = (30, 30)
  private var internalState = World.withRandomCoins(worldSize, coinRatio = 0.3)


  override def onCreate(): Unit = {
    this.startStateUpdate()
  }

  override def onClose(): Unit = {}

  override def onJoin(client: Client): Unit = {
    this.players = this.players + (client.id -> this.players.size)
    val newPlayer = Player(this.players(client.id), (0, 0), 0)
    this.internalState = World(
      this.internalState.players :+ newPlayer,
      this.internalState.coins,
      this.internalState.size)
  }

  override def onLeave(client: Client): Unit = {
    this.internalState = World(
      this.internalState.players.filter(_.id != players(client.id)),
      this.internalState.coins,
      this.internalState.size)
    this.players = this.players - client.id

  }

  override def onMessageReceived(client: Client, message: Any): Unit = {
    println("msg received")
    val direction: Direction = message.asInstanceOf[Direction]
    val playerId: Int = this.players(client.id)
    this.internalState = World(internalState.players.collect({
      case p if p.id == playerId =>
        println("found")
        Player(p.id, p.move(direction), p.points)
      case p => p
    }), internalState.coins, internalState.size).takeCoins()

  }

  override def currentState: World = this.internalState

}
