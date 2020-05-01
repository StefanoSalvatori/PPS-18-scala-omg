package scalaomg.examples.roll_the_dice.client.view.scenes

import java.awt.{BorderLayout, Font}

import scalaomg.examples.roll_the_dice.client.view.{Utils, View}
import javax.swing._

case class Menu(private val view: View) extends BasicScene {

  // Set title
  private val titleLabelFont = new Font(new JLabel().getName, Font.PLAIN, 30) // scalastyle:ignore magic.number
  private val titleLabel = new JLabel("Roll The Dice")
  titleLabel setFont titleLabelFont
  panel add (titleLabel, BorderLayout.NORTH)

  // Set main menu
  import scalaomg.examples.roll_the_dice.client.view.Utils.DimensionConverters._
  private val boxSpacing = (20, 20)
  private val menuBox = Box.createVerticalBox()
  private val quitButton = new JButton("Quit")
  private val findMatchButton = new JButton("Play")

  menuBox add Utils.spacing(boxSpacing)
  menuBox add findMatchButton
  menuBox add Utils.spacing(boxSpacing)
  menuBox add quitButton

  panel.add(menuBox, BorderLayout.CENTER)

  findMatchButton addActionListener { _ => view.showMatchSetup() }
  quitButton addActionListener { _ => System exit 0 }
}
