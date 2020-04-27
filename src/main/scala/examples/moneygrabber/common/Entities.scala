package examples.moneygrabber.common

import scala.util.Random

object Entities {
  type Position = (Int, Int)
  type ContinuousPosition = (Double, Double)


  def continuousToDiscrete(pos: ContinuousPosition): Position = (Math.round(pos._1).toInt, Math.round(pos._2).toInt)

  implicit def discreteToContinuous(pos: Position): ContinuousPosition = (pos._1.toDouble, pos._2.toDouble)

  @SerialVersionUID(3463L) // scalastyle:ignore magic.number
  sealed trait Direction extends java.io.Serializable
  case object Up extends Direction
  case object Right extends Direction
  case object Down extends Direction
  case object Left extends Direction
  val Directions: List[Direction] = List(Up, Right, Down, Left)

  def randomDirection: Direction = Random.shuffle(Directions).head

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

    def moveRandom(): Position = {
      this.move(Random.shuffle(Directions).head)
    }
  }

  implicit val playerOrder: Ordering[Player] = (x: Player, y: Player) => x.points - y.points
  @SerialVersionUID(1234L) // scalastyle:ignore magic.number
  case class Player(id: Int, position: Position, points: Int) extends Entity with java.io.Serializable

  @SerialVersionUID(23532L) // scalastyle:ignore magic.number
  case class Coin(position: Position, value: Int) extends Entity with java.io.Serializable

  //hunters need to have a (double,double) position since they can move fraction of tiles
  @SerialVersionUID(45632L) // scalastyle:ignore magic.number
  case class Hunter(continuousPosition: ContinuousPosition, direction: Direction, velocity: Double)
    extends Entity with java.io.Serializable {
    override val position: (Int, Int) = continuousToDiscrete(this.continuousPosition)

    def moveContinuous(elapsed: Long): ContinuousPosition = {
      val x = continuousPosition._1
      val y = continuousPosition._2

      val seconds: Double = elapsed.toDouble / 1000 / 2
      //half speed because rounding from double to int reach next int every 0.5 so we move to next tile too often
      direction match {
        case Up => (x, y + seconds * velocity)
        case Right => (x + seconds * velocity, y)
        case Down => (x, y - seconds * velocity)
        case Left => (x - seconds * velocity, y)
      }
    }

  }

}

