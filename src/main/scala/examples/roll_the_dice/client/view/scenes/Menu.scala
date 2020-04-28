package examples.roll_the_dice.client.view.scenes

import java.awt.BorderLayout

import examples.roll_the_dice.client.view.View
import javax.swing._

case class Menu(private val view: View) extends BasicScene {

  // Set title
  panel add (new JLabel("Roll The Dice"), BorderLayout.NORTH)

  // Set main menu
  private val menuBox = Box.createVerticalBox()
  private val quitButton = new JButton("Quit")
  private val findMatchButton = new JButton("Play w/ default matchmaker")
  menuBox add findMatchButton
  menuBox add quitButton
  panel.add(menuBox, BorderLayout.CENTER)

  findMatchButton addActionListener { _ => view.showMatchSetup() }
  quitButton addActionListener { _ => System exit 0 }
}
