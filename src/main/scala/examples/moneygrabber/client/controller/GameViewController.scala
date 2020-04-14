package examples.moneygrabber.client.controller

import client.room.ClientRoom
import examples.moneygrabber.client.view.game.GameFrame.GameFrameClosed
import examples.moneygrabber.client.view.game.GameGrid.ButtonPressedEvent
import examples.moneygrabber.client.view.game.{GameFrame, GameView}
import examples.moneygrabber.common.Board
import examples.moneygrabber.common.Entities.{Direction, Down, Left, Right, Up}
import javax.swing.SwingUtilities

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.swing.event.Key
import scala.swing.{Dialog, Publisher}
import scala.util.{Failure, Success}

object GameViewController {
  def keyToAction(key: Key.Value): Direction = key match {
    case Key.Left => Left
    case Key.Right => Right
    case Key.Up => Up
    case Key.Down => Down
  }
}

case class GameViewController(private val frame: GameFrame, private val room: ClientRoom) extends Publisher {

  import GameViewController._
  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global
  val view: GameView = this.frame.gameView
  listenTo(this.view, this.frame)
  reactions += {
    case ButtonPressedEvent(key) => this.room.send(keyToAction(key))
    case GameFrameClosed => this.room.leave() onComplete {
      case Success(value) => println("leave ok")
      case Failure(_) => println("leave error")
    }
  }

  room.onMessageReceived {
    case id: Int => SwingUtilities.invokeLater(() => {
      this.view.setPlayerColor(id)
    })
    case endState: Board => this.onGameEnd(endState)
  }

  room.onStateChanged(state => {
    val gameState = state.asInstanceOf[Board]
    if (!gameState.gameEnded) {
      this.updateView(gameState)
    }
  })

  def openGameView(): Unit = this.frame.show()

  private def updateView(gameState: Board): Unit = {
    SwingUtilities.invokeLater(() => {
      view.clearTiles()
      gameState.coins.foreach(c => view.colorCoinTile(c.position))
      gameState.players.foreach(p => view.colorPlayerTile(p.id, p.position))
      view.showPlayersPoints(gameState.players.map(p => (p.id, p.points)).toMap)
    })
  }

  private def onGameEnd(gameState: Board): Unit = {
    SwingUtilities.invokeLater(() => {
      import examples.moneygrabber.client.view.Utils._
      room.leave()
      Dialog.showMessage(view,
        s"${view.PlayerIdToColor(gameState.winner.id).name} player won",
        "Game Ended",
        Dialog.Message.Plain)
      frame.close()
    })

  }
}
