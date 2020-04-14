package examples.moneygrabber.server.rooms

import examples.moneygrabber.common.Entities.{Direction, Player}
import examples.moneygrabber.common.{Board, GameModes}
import server.room.{Client, ServerRoom, SynchronizedRoomState}

case class MatchRoom() extends ServerRoom with SynchronizedRoomState[Board] {
  val size: Int = 20
  val mode: String = GameModes.Max2.name

  private var gameState = Board.withRandomCoins((size, size), coinRatio = 0.1)
  //map clientId -> playerId
  private var players: Map[String, Int] = Map.empty
  private var gameStarted = false


  override def onCreate(): Unit = {}

  override def onClose(): Unit = {}

  override def onJoin(client: Client): Unit = {
    this.players = this.players + (client.id -> this.players.size)
    val newPlayer = Player(this.players(client.id), playerStartingPosition, 0)
    this.gameState = this.gameState.addPlayer(newPlayer)
    tell(client, this.players(client.id)) // tells the client the id that he got
    if (canStart) {
      this.gameStarted = true
      this.startStateUpdate()
    }
  }

  override def onLeave(client: Client): Unit = {
    this.gameState = this.gameState.removePlayer(this.players(client.id))
    this.players = this.players - client.id
  }

  override def onMessageReceived(client: Client, message: Any): Unit = {
    if (this.gameStarted) {
      val direction: Direction = message.asInstanceOf[Direction]
      val playerId: Int = this.players(client.id)
      this.gameState = this.gameState.movePlayer(playerId, direction).takeCoins()
      if(this.gameState.gameEnded) {
        broadcast(gameState)
        this.close()
      }
    }
  }

  override def currentState: Board = this.gameState

  override def joinConstraints: Boolean = this.mode match {
    case GameModes.Max2.name => this.connectedClients.size < GameModes.Max2.numPlayers
    case GameModes.Max4.name => this.connectedClients.size < GameModes.Max4.numPlayers
  }

  private def canStart = this.mode match {
    case GameModes.Max2.name => this.connectedClients.size == GameModes.Max2.numPlayers
    case GameModes.Max4.name => this.connectedClients.size == GameModes.Max4.numPlayers
    case _ => true
  }

  private def playerStartingPosition = this.connectedClients.size match {
    case 1 => (0, 0)
    case 2 => (size - 1, size - 1)
    case 3 => (size - 1, 0)
    case 4 => (0, size - 1)
  }

}
