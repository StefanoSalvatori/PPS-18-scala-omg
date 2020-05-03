package scalaomg.examples.roll_the_dice.client.controller

import scalaomg.examples.roll_the_dice.client.model.Model
import scalaomg.examples.roll_the_dice.common.ServerInfo

object Main extends App {
  private val host = if (args.length > 0) args(0) else ServerInfo.DefaultHost
  private val port = if (args.length > 1) args(1).toInt else ServerInfo.DefaultPort
  Controller(ServerInfo(host, port)).start()
}

trait Controller {

  def start(): Unit

  def closeApplication(): Unit

  import scalaomg.examples.roll_the_dice.common.Team
  def joinGameWithMatchmaking(desiredTeam: Team): Unit

  def leaveMatchmakingQueue(): Unit

  def rollDice(): Unit
}

object Controller {
  def apply(serverInfo: ServerInfo): Controller = ControllerImpl(serverInfo)
}

import scalaomg.examples.roll_the_dice.client.view.View
import scalaomg.examples.roll_the_dice.client._
import scalaomg.examples.roll_the_dice.common.Team

case class ControllerImpl(serverInfo: ServerInfo) extends Controller with Subscriber {

  private val model = Model(serverInfo)
  private val view = View(this)

  subscribe()

  override def start(): Unit = {
    model.start()
    view.startGUI()
  }

  override def closeApplication(): Unit = model.leaveRoom()

  override def joinGameWithMatchmaking(desiredTeam: Team): Unit = model.joinGameWithMatchmaking(desiredTeam)

  override def leaveMatchmakingQueue(): Unit = model.leaveMatchmakingQueue()

  override def rollDice(): Unit = model.rollDice()

  override def onItemPublished(message: PubSubMessage): Unit = message match {
    case PubSubRoomState(newState) =>
      view updateState newState
    case PubSubSetupGame(assignedTurn, startingState, goalPoints) =>
      view startGame(assignedTurn, startingState, goalPoints)
    case PubSubNextTurn(newTurn) =>
      view changeTurn newTurn
    case PubSubWin(team) =>
      view endGame team
    case LeftMatchmaking =>
      view.showMainMenu()
    case LeftRoom =>
      System exit 0
  }
}
