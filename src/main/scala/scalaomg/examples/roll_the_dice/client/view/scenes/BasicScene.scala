package scalaomg.examples.roll_the_dice.client.view.scenes

import java.awt.BorderLayout

import javax.swing.JPanel

trait BasicScene {

  private val _panel = new JPanel()
  private val layout = new BorderLayout()
  _panel setLayout layout

  def panel: JPanel = _panel
}
