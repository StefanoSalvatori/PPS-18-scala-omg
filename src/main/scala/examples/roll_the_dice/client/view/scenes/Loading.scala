package examples.roll_the_dice.client.view.scenes

import java.awt.BorderLayout

import examples.roll_the_dice.client.view.{Utils, View}
import javax.swing.{Box, BoxLayout, JButton, JLabel}

case class Loading(private val view: View) extends BasicScene {

  import examples.roll_the_dice.client.view.Utils.DimensionConverters._
  private val boxSpacing = (10, 1)

  private val box = new Box(BoxLayout.Y_AXIS)
  private val loadingLabel = new JLabel("Looking for a game to join...")
  private val backButton = new JButton("Back")
  box add loadingLabel
  box add Utils.spacing(boxSpacing)
  box add backButton
  panel add (box, BorderLayout.CENTER)

  backButton addActionListener { _ => view.leaveMatchmakingQueue() }
}
