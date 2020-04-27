package examples.roll_the_dice.client.view.scenes

import java.awt.{BorderLayout, Color, Font}

import examples.roll_the_dice.client.view.View
import examples.roll_the_dice.common.{MatchState, Turn}
import javax.swing.{Box, JLabel}

import scala.swing.Dimension

case class Match(private val view: View) extends BasicScene {

  // My turn
  private var _assignedTurn: Turn = _
  private val myTurnLabel = new JLabel("")

  // Points box
  val pointsLabelFont = new Font(new JLabel().getName, Font.PLAIN, 30) // scalastyle:ignore magic.number
  private val pointsBox = Box.createHorizontalBox()
  private val redTeamName = "Blue team"
  private val blueTeamName = "Red team"
  private val redTeamLabel = new JLabel(redTeamName)
  private val blueTeamLabel = new JLabel(blueTeamName)
  redTeamLabel setFont pointsLabelFont
  redTeamLabel setForeground Color.RED
  blueTeamLabel setFont pointsLabelFont
  blueTeamLabel setForeground Color.BLUE

  pointsBox add redTeamLabel
  pointsBox add (Box createRigidArea new Dimension(30,30)) // scalastyle:ignore magic.number
  pointsBox add blueTeamLabel
  panel add (pointsBox, BorderLayout.NORTH)

  def assignedTurn_=(turn: Turn): Unit = {
    _assignedTurn = turn
    myTurnLabel setText _assignedTurn.toString
  }

  def assignedTurn: Turn = _assignedTurn

  def updateState(newState: MatchState): Unit = {
    redTeamLabel setText redTeamName + " " + newState.pointsA
    blueTeamLabel setText newState.pointsB + " " + blueTeamName
  }
}

private case class TeamToolbar() {

  private val _box = Box.createVerticalBox()
  private val player1 = PlayerBox()
  private val player2 = PlayerBox()

  def box: Box = _box
}

private case class PlayerBox() extends BasicScene {

  private val _box = Box.createVerticalBox()

  def box: Box = box
}
