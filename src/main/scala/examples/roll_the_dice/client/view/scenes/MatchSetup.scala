package examples.roll_the_dice.client.view.scenes

import java.awt.BorderLayout

import examples.roll_the_dice.client.view.View
import examples.roll_the_dice.common.{A, B}
import javax.swing.{JButton, JLabel}

case class MatchSetup(private val view: View) extends BasicScene {

  private val teamChooser = TeamBox(view)

  panel add (teamChooser.box, BorderLayout.CENTER)
}

private case class TeamBox(private val view: View) extends BasicBox {

  case class Teams() extends BasicBox {

    private val redTeam = new JButton("RED")
    private val blueTeam = new JButton("BLUE")

    redTeam addActionListener { _ => view.joinGameWithMatchmaking(A) }
    blueTeam addActionListener { _ => view.joinGameWithMatchmaking(B) }

    box add redTeam
    box add blueTeam

    override def boxType: BoxType = HorizontalBox
  }

  private val chooseTeamLabel = new JLabel("Choose you desired team:")
  private val teams = Teams()

  box add chooseTeamLabel
  box add teams.box

  override def boxType: BoxType = VerticalBox
}

