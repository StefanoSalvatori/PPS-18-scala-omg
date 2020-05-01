package scalaomg.examples.moneygrabber.common

import scalaomg.examples.moneygrabber.common.Entities.{Coin, Direction, Directions, Hunter, Player, Position}
import Entities._

import scala.util.Random


object Board {

  val CoinBasicValue = 10
  val HunterDropValue = 30
  val HunterVelocity = 3.5
  val HunterChangeDirectionProbability = 0.2
  val HunterDropProbability = 0.008 //every time an hunter moves has a certain probability to drop coins


  def withRandomCoins(size: (Int, Int), numHunters: Int, coinRatio: Double): Board = {
    val huntersList = hunters(size, numHunters)
    println(huntersList)
    Board(List.empty,
      randomCoins(size, coinRatio),
      huntersList,
      size)
  }

  private def randomCoins(worldSize: (Int, Int), coinRatio: Double): List[Coin] = {
    val pairs = for (x <- 0 until worldSize._1; y <- 0 until worldSize._2) yield (x, y)
    Random.shuffle(pairs).take((pairs.size * coinRatio).toInt).map(Coin(_, CoinBasicValue)).toList
  }

  private def hunters(size: (Int, Int), numHunters: Int): List[Hunter] = {
    (0 until numHunters).map(_ => Hunter((size._1 / 2, size._2 / 2), randomDirection, HunterVelocity)).toList
  }
}

@SerialVersionUID(1111L) // scalastyle:ignore magic.number
case class Board(players: List[Player], coins: List[Coin], hunters: List[Hunter], size: (Int, Int))
  extends java.io.Serializable {

  import Board._

  def gameEnded: Boolean = this.coins.isEmpty || this.players.size == 1

  def winner: Player = if (this.players.size == 1) {
    this.players.head
  } else {
    this.players.max
  }

  def addPlayer(player: Player): Board = {
    if (this.players.exists(_.id == player.id)) {
      throw new IllegalStateException(s"There's already a player with id:${player.id}")
    }
    this.copy(players = this.players :+ player)
  }

  def removePlayer(id: Int): Board = {
    this.copy(players = this.players.filter(_.id != id))
  }

  def movePlayer(id: Int, direction: Direction): Board = {
    this.copy(players = this.players.map(p => {
      if (p.id == id) {
        Player(p.id, this.keepInsideBorders(p.move(direction)), p.points)
      } else {
        p
      }
    })).takeCoins().catchPlayers()
  }

  //Return the board with hunters that moved randomly and possibly drop coins
  def moveHunters(elapsed: Long): Board = {
    var dropped: List[Coin] = List.empty
    this.copy(hunters = this.hunters.collect({
      case h if continuousToDiscrete(h.moveContinuous(elapsed)) == h.position =>
        h.copy(continuousPosition = h.moveContinuous(elapsed))
      case h =>
        if (hunterDropped) {
          dropped = Coin(h.position, HunterDropValue) :: dropped
        }
        h.copy(continuousPosition = this.keepInsideBorders(continuousToDiscrete(h.moveContinuous(elapsed))),
          direction = randomlyChangeDirection(h))
    }), coins = this.coins ++ dropped).catchPlayers()
  }

  //Return the world with players score updated according to taken coins
  def takeCoins(): Board = {
    val newCoins = coins.filter(c => !players.exists(p => p.position == c.position))
    val newPlayers = players.map(p => coins.find(c => c.position == p.position) match {
      case Some(coin) => Player(p.id, p.position, p.points + coin.value)
      case None => p
    })
    this.copy(players = newPlayers, coins = newCoins)
  }

  //Return the board without the caught players
  def catchPlayers(): Board = {
    this.copy(players = this.players.filter(!playerCaught(_)))
  }

  private def randomlyChangeDirection(hunter: Hunter): Direction = {
    if (Random.nextDouble < HunterChangeDirectionProbability) {
      randomDirection
    }
    else {
      hunter.direction
    }
  }

  private def hunterDropped = Random.nextDouble < HunterDropProbability

  private def playerCaught(player: Player) = hunters.map(_.position).contains(player.position)

  private def keepInsideBorders(position: Position): Position = {
    (keepXInsideBorders(position._1), keepYInsideBorders(position._2))
  }

  private def keepXInsideBorders(x: Int) = {
    Math.min(Math.max(0, x), this.size._1 - 1)
  }

  private def keepYInsideBorders(y: Int) = {
    Math.min(Math.max(0, y), this.size._2 - 1)
  }
}