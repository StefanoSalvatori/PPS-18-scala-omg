package examples.roll_the_dice.client.view

import javax.swing.JButton

import scala.swing.Dimension

private case class BasicButton(text: String, _size: Dimension) extends JButton {

  super.setText(text)
  super.setPreferredSize(_size)
}