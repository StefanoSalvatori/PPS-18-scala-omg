package examples.moneygrabber.common


object GameModes {
  case class GameMode(name: String, numPlayers: Int)

  val Max2: GameMode = GameMode("2max", 2) // scalastyle:ignore magic.number
  val Max4: GameMode = GameMode("4max", 4) // scalastyle:ignore magic.number
}
