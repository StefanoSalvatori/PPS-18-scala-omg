package examples.roll_the_dice.common

object MessageDictionary {

  case class StartGame(assignedTurn: Turn)

  case class Roll()

  case class DoRoll(currentTurn: Turn)
}
