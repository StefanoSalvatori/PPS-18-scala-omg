package scalaomg.examples.moneygrabber.client.view.game

import java.awt.Dimension

import scalaomg.examples.moneygrabber.client.view.game.GameFrame.GameFrameClosed

import scala.swing.Frame
import scala.swing.event.Event

object GameFrame {
  object GameFrameClosed extends Event
}

case class GameFrame(gameView: GameView) extends Frame {

  title = "Money Grabber"
  contents = gameView
  resizable = false

  def show(): Unit = {
    pack()
    centerOnScreen()
    open()
  }

  override def closeOperation(): Unit = {
    publish(GameFrameClosed)
    super.closeOperation()
  }

}
