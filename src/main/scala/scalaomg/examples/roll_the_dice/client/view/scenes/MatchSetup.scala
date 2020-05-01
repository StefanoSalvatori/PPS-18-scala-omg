package scalaomg.examples.roll_the_dice.client.view.scenes

import java.awt.{BorderLayout, Font}

import scalaomg.examples.roll_the_dice.client.view.{Utils, View}
import scalaomg.examples.roll_the_dice.common.{A, B}
import javax.swing.{Box, JButton, JLabel}

case class MatchSetup(private val view: View) extends BasicScene {

  private val titleLabelFont = new Font(new JLabel().getName, Font.PLAIN, 30) // scalastyle:ignore magic.number
  private val title = new JLabel("Match setup")
  title setFont titleLabelFont

  private val teamChooser = TeamBox(view)

  panel add (title, BorderLayout.NORTH)
  panel add (teamChooser.box, BorderLayout.CENTER)
}

private case class TeamBox(private val view: View) extends BasicBox {

  import scalaomg.examples.roll_the_dice.client.view.Utils.DimensionConverters._
  private val boxSpacing = (20, 20)

  private val chooseTeamLabel = new JLabel("Choose you desired team:")
  private val redTeam = new JButton("RED")
  private val blueTeam = new JButton("BLUE")

  redTeam addActionListener { _ => view.joinGameWithMatchmaking(A) }
  blueTeam addActionListener { _ => view.joinGameWithMatchmaking(B) }

  box add chooseTeamLabel
  box add Utils.spacing(boxSpacing)
  box add redTeam
  box add Utils.spacing(boxSpacing)
  box add blueTeam

  override def boxType: BoxType = HorizontalBox
}

