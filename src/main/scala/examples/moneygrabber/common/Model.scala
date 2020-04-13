package examples.moneygrabber.common

import examples.moneygrabber.common.Entities.{Coin, Player}

import scala.util.Random


object Model {
  type Position = (Int, Int)

  @SerialVersionUID(12453L) // scalastyle:ignore magic.number
  sealed trait Direction extends java.io.Serializable
  case object Up extends Direction
  case object Right extends Direction
  case object Down extends Direction
  case object Left extends Direction


  object World {

    import Entities._
    import Entities.Coin._
    val DefaultCoinRatio = 0.5

    def withRandomCoins(size: (Int, Int), coinRatio: Double = DefaultCoinRatio): World =
      World(List.empty, randomCoins(size, coinRatio).toList, size)

    private def randomCoins(worldSize: (Int, Int), coinRatio: Double = DefaultCoinRatio): Seq[Coin] = {
      val pairs = for (x <- 0 to worldSize._1; y <- 0 to worldSize._2) yield (x, y)
      Random.shuffle(pairs).take((pairs.size * coinRatio).toInt).map(Coin(_, CoinBasicValue))
    }
  }

  @SerialVersionUID(1111L) // scalastyle:ignore magic.number
  case class World(players: List[Player], coins: List[Coin], size: (Int, Int)) extends java.io.Serializable {
    def takeCoins(): World = {
      val newCoins = coins.filter(c => !players.exists(p => p.position == c.position))
      val newPlayers = players.map(p => coins.find(c => c.position == p.position) match {
        case Some(coin) => Player(p.id, p.position, p.points + coin.value)
        case None => p
      })
      World(newPlayers, newCoins, size)
    }
  }
}