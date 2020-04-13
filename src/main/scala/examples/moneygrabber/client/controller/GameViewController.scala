package examples.moneygrabber.client.controller

import java.awt.Color  // scalastyle:ignore illegal.imports

import client.room.ClientRoom
import examples.moneygrabber.client.view.GameGrid.ButtonPressedEvent
import examples.moneygrabber.client.view.GameView
import examples.moneygrabber.common.Model
import examples.moneygrabber.common.Model.World
import javax.swing.SwingUtilities

import scala.swing.Publisher
import scala.swing.event.Key

object GameViewController {
  def keyToAction(key: Key.Value): Model.Direction = key match {
    case Key.Left => Model.Left
    case Key.Right => Model.Right
    case Key.Up => Model.Up
    case Key.Down => Model.Down
  }

}

case class GameViewController(private val view: GameView, private val room: ClientRoom) extends Publisher {

  import GameViewController._

  listenTo(view.gameGrid)
  reactions += {
    case ButtonPressedEvent(key) => this.room.send(keyToAction(key))
  }

  room.onStateChanged(state => {
    val gameState = state.asInstanceOf[World]
    SwingUtilities.invokeLater(() => {
      view.gameGrid.resetGrid()
      gameState.coins.map(_.position).foreach(view.gameGrid.colorButton(_, Color.yellow))
      gameState.players.map(_.position).foreach(view.gameGrid.colorButton(_, Color.red))
      view.setPlayerPoints(gameState.players.map(_.points))
    })
  })
}
