package examples.roll_the_dice.common

object MessageDictionary {

  case class StartGame(assignedTurn: Turn, startingState: MatchState, goalPoints: Int)

  case object Roll

  case class NextTurn(currentTurn: Turn)

  case class Win(team: Team)
}
