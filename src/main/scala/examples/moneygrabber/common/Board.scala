package examples.moneygrabber.common

import examples.moneygrabber.common.Entities.{Coin, Direction, Player, Position}

import scala.util.Random


object Board {

  import Entities._
  import Entities.Coin._

  def withRandomCoins(size: (Int, Int), coinRatio: Double): Board =
    Board(List.empty, randomCoins(size, coinRatio).toList, size)

  private def randomCoins(worldSize: (Int, Int), coinRatio: Double): Seq[Coin] = {
    val pairs = for (x <- 0 until  worldSize._1; y <- 0 until  worldSize._2) yield (x, y)
    Random.shuffle(pairs).take((pairs.size * coinRatio).toInt).map(Coin(_, CoinBasicValue))
  }
}

@SerialVersionUID(1111L) // scalastyle:ignore magic.number
case class Board(players: List[Player], coins: List[Coin], size: (Int, Int)) extends java.io.Serializable {
  def takeCoins(): Board = {
    val newCoins = coins.filter(c => !players.exists(p => p.position == c.position))
    val newPlayers = players.map(p => coins.find(c => c.position == p.position) match {
      case Some(coin) => Player(p.id, p.position, p.points + coin.value)
      case None => p
    })
    Board(newPlayers, newCoins, size)
  }

  def gameEnded: Boolean = this.coins.isEmpty

  def winner: Player = this.players.max

  def addPlayer(player: Player): Board = {
    if (this.players.exists(_.id == player.id)) {
      throw new IllegalStateException(s"There's already a player with id:${player.id}")
    }
    Board(this.players :+ player, this.coins, this.size)
  }

  def removePlayer(id: Int): Board = {
    Board(this.players.filter(_.id != id), this.coins, this.size)
  }

  def movePlayer(id: Int, direction: Direction): Board = {
    Board(this.players.map(p => {
      if (p.id == id) {
        Player(p.id, this.keepInsideBorders(p.move(direction)), p.points)
      } else {
        p
      }
    }), this.coins, this.size)
  }

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