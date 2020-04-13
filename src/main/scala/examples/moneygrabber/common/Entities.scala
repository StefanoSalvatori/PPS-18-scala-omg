package examples.moneygrabber.common

import examples.moneygrabber.common.Model.{Direction, Down, Left, Position, Right, Up}

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

object Entities {
  @SerialVersionUID(1234L) // scalastyle:ignore magic.number
  case class Player(id: Int, position: Position, points: Int) extends Entity with java.io.Serializable

  object Coin {
    val CoinBasicValue = 10
  }

  @SerialVersionUID(23532L) // scalastyle:ignore magic.number
  case class Coin(position: Position, value: Int) extends Entity with java.io.Serializable

}
