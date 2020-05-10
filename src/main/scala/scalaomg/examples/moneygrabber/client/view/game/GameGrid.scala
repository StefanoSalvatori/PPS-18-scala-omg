package scalaomg.examples.moneygrabber.client.view.game

import java.awt.Color

import scala.swing.GridBagPanel.Fill
import scala.swing.event.{Event, Key}
import scala.swing.{Button, Dimension, GridBagPanel}

object GameGrid {
  val GridDefaultColor: Color = Color.GRAY
  case class ButtonPressedEvent(key: Key.Value) extends Event
}

case class GameGrid(gridSize: (Int, Int), tileSize: Int) extends GridBagPanel {

  import GameGrid._
  private var gridButtons: Map[(Int, Int), Button] = Map.empty
  private val c = new Constraints
  focusable = true
  c.fill = Fill.Both
  drawGrid()

  def resetGrid(): Unit = this.gridButtons.values.foreach(_.background = GridDefaultColor)

  def colorButton(position: (Int, Int), color: Color): Unit = {
    if (this.gridButtons.isDefinedAt(position)) {
      this.gridButtons(position).background = color
    }
  }

  private def drawGrid(): Unit = {
    for (x <- 0 until gridSize._1; y <- 0 until gridSize._2) {
      val button = newButton()
      gridButtons = gridButtons + ((x, gridSize._2 - 1 - y) -> button)
      c.grid = (x, y)
      layout(button) = c
    }

  }

  private def newButton() = new Button {
    enabled = false
    background = GridDefaultColor
    preferredSize = new Dimension(tileSize, tileSize)
  }

}
