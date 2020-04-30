package examples.roll_the_dice.client.controller

object Main extends App {
  Controller().start()
}

object Main2 extends App {
  Controller().start()
}

object Main3 extends App {
  Controller().start()
}

object Main4 extends App {
  Controller().start()
}

trait Controller {

  def start(): Unit

  def closeApplication(): Unit

  import examples.roll_the_dice.common.Team
  def joinGameWithMatchmaking(desiredTeam: Team): Unit

  def leaveMatchmakingQueue(): Unit

  def rollDice(): Unit
}

object Controller {
  def apply(): Controller = ControllerImpl()
}

import examples.roll_the_dice.client.{PubSubMessage, PubSubNextTurn, PubSubRoomState, PubSubSetupGame, PubSubWin, Subscriber}
import examples.roll_the_dice.client.model.Model
import examples.roll_the_dice.client.view.View
import examples.roll_the_dice.common.Team

case class ControllerImpl() extends Controller with Subscriber {

  private val model = Model()
  private val view = View(this)

  subscribe()

  override def start(): Unit = {
    model.start()
    view.startGUI()
  }

  override def closeApplication(): Unit = {
    // TODO close client?
    System exit 0
  }

  override def joinGameWithMatchmaking(desiredTeam: Team): Unit = model.joinGameWithMatchmaking(desiredTeam)

  override def leaveMatchmakingQueue(): Unit = model.leaveMatchmakingQueue()

  override def rollDice(): Unit = model.rollDice()

  override def onItemPublished(message: PubSubMessage): Unit = message match {
    case PubSubRoomState(newState) =>
      view updateState newState
    case PubSubSetupGame(assignedTurn, startingState, goalPoints) =>
      view startGame (assignedTurn, startingState, goalPoints)
    case PubSubNextTurn(newTurn) =>
      view changeTurn newTurn
    case PubSubWin(team) =>
      view endGame team

  }
}
