package scalaomg.examples.roll_the_dice.common

@SerialVersionUID(1234L) // scalastyle:ignore magic.number
case class MatchState(pointsA: Int = 0, pointsB: Int = 0) extends java.io.Serializable {

  def addPointsA(points: Int): MatchState = MatchState(pointsA + points, pointsB)

  def addPointsB(points: Int): MatchState = MatchState(pointsA, pointsB + points)
}
