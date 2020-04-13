package examples.moneygrabber.client.view

import java.awt.Color  // scalastyle:ignore illegal.imports

import scala.swing.GridBagPanel.Fill
import scala.swing.event.{Event, Key, KeyPressed}
import scala.swing.{Button, Dimension, GridBagPanel}

object GameGrid {
  val GridDefaultColor: Color = Color.GRAY
  case class ButtonPressedEvent(key: Key.Value) extends Event
}

case class GameGrid(gridSize: (Int, Int), tileSize: Int) extends GridBagPanel {

  import GameGrid._

  private var gridButtons: Map[(Int, Int), Button] = Map.empty

  this.focusable = true
  listenTo(keys)
  reactions += {
    case event: KeyPressed => publish(ButtonPressedEvent(event.key))
  }
  val c = new Constraints
  c.fill = Fill.Both
  for (x <- 0 until gridSize._1; y <- 0 until gridSize._2) {
    val button = newButton()
    gridButtons = gridButtons + ((x, gridSize._2 - 1 - y) -> button)
    c.gridx = x; c.gridy = y
    layout(button) = c
  }

  def resetGrid(): Unit = this.gridButtons.values.foreach(_.background = GridDefaultColor)

  def colorButton(position: (Int, Int), color: Color): Unit = {
    if (this.gridButtons.isDefinedAt(position)) {
      this.gridButtons(position).background = color
    }
  }

  private def newButton() = new Button {
    background = GridDefaultColor
    preferredSize = new Dimension(tileSize, tileSize)
  }

}
