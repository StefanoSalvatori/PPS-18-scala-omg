package examples.moneygrabber.common

object Entities {
  type Position = (Int, Int)

  @SerialVersionUID(3463L) // scalastyle:ignore magic.number
  sealed trait Direction extends java.io.Serializable
  case object Up extends Direction
  case object Right extends Direction
  case object Down extends Direction
  case object Left extends Direction

  trait Entity {
    val position: Position

    def x: Int = this.position._1

    def y: Int = this.position._2

    def move(direction: Direction): Position = direction match {
      case Up => (x, y + 1)
      case Right => (x + 1, y)
      case Down => (x, y - 1)
      case Left => (x - 1, y)
    }
  }

  implicit val playerOrder: Ordering[Player] = (x: Player, y: Player) => x.points - y.points
  @SerialVersionUID(1234L) // scalastyle:ignore magic.number
  case class Player(id: Int, position: Position, points: Int) extends Entity with java.io.Serializable

  object Coin {
    val CoinBasicValue = 10
  }

  @SerialVersionUID(23532L) // scalastyle:ignore magic.number
  case class Coin(position: Position, value: Int) extends Entity with java.io.Serializable

}

