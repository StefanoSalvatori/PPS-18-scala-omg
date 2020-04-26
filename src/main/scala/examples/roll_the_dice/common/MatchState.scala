package examples.roll_the_dice.common

@SerialVersionUID(1234L) // scalastyle:ignore magic.number
case class MatchState(pointsA: Int, pointsB: Int) extends java.io.Serializable {

  def addPointsA(points: Int): MatchState = MatchState(pointsA + points, pointsB)

  def addPointsB(points: Int): MatchState = MatchState(pointsA, pointsB + points)
}
