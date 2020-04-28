package examples.roll_the_dice.client.view.scenes

import java.awt.{BorderLayout, Color, Font}

import examples.roll_the_dice.client.view.{View, WindowSize}
import examples.roll_the_dice.common.{A1, A2, B1, B2, MatchState, Turn}
import javax.swing.{BorderFactory, Box, JLabel}
import examples.roll_the_dice.client.view.Utils.DimensionConverters._

case class Match(private val view: View) extends BasicScene {

  // Points box
  private val pointsBox = PointsBox()
  panel add (pointsBox.box, BorderLayout.NORTH)

  // Players toolbars
  private val redTeamToolbar = TeamToolbar(A1, A2)
  private val blueTeamToolbar = TeamToolbar(B1, B2)
  panel add (redTeamToolbar.box, BorderLayout.WEST)
  panel add (blueTeamToolbar.box, BorderLayout.EAST)

  // Middle panel
  private val middleBox = MiddleBox()
  panel add (middleBox.box, BorderLayout.CENTER)

  def assignedTurn_=(turn: Turn): Unit = middleBox.assignedTurn = turn

  def assignedTurn: Turn = middleBox.assignedTurn

  def updateState(newState: MatchState): Unit = {
    pointsBox.redTeamLabel setText pointsBox.redTeamName + " " + newState.pointsA
    pointsBox.blueTeamLabel setText newState.pointsB + " " + pointsBox.blueTeamName
  }
}

private case class PointsBox() extends BasicBox {

  val pointsLabelFont = new Font(new JLabel().getName, Font.PLAIN, 30) // scalastyle:ignore magic.number
  val redTeamName = "Blue team"
  val blueTeamName = "Red team"
  val redTeamLabel = new JLabel(redTeamName)
  val blueTeamLabel = new JLabel(blueTeamName)
  redTeamLabel setFont pointsLabelFont
  redTeamLabel setForeground Color.RED
  blueTeamLabel setFont pointsLabelFont
  blueTeamLabel setForeground Color.BLUE

  box setBorder BorderFactory.createEtchedBorder(1)
  box setPreferredSize (WindowSize.windowWidth * 0.5, WindowSize.windowHeight * 0.2)
  box add redTeamLabel
  box add (Box createRigidArea (30, 30)) // scalastyle:ignore magic.number
  box add blueTeamLabel

  override def boxType: BoxType = HorizontalBox
}

private case class TeamToolbar(player1: Turn, player2: Turn) extends BasicBox {

  private val spacingDimension = (20, 20) // scalastyle:ignore magic.number

  private val topSpacing = Box createRigidArea spacingDimension
  private val player1Box = PlayerBox(player1)
  private val middleSpacing = Box createRigidArea spacingDimension
  private val player2Box = PlayerBox(player2)
  private val bottomSpacing = Box createRigidArea spacingDimension

  box add topSpacing
  box add player1Box.box
  box add middleSpacing
  box add player2Box.box
  box add bottomSpacing

  box setBorder BorderFactory.createEtchedBorder(1)

  override def boxType: BoxType = VerticalBox

  private val boxDimension = (WindowSize.windowWidth * 0.33, WindowSize.windowHeight * 0.5)
  box setPreferredSize boxDimension

}

private case class PlayerBox(turn: Turn) extends BasicBox {

  val playerLabelFont = new Font(new JLabel().getName, Font.PLAIN, 20) // scalastyle:ignore magic.number
  private val name = new JLabel(turn.toString)
  name setFont  playerLabelFont
  name setForeground (turn match {
    case A1 | A2 => Color.RED
    case B1 | B2 => Color.BLUE
  })

  box add name

  override def boxType: BoxType = VerticalBox
}

private case class MiddleBox() extends BasicBox {

  val labelFont = new Font(new JLabel().getName, Font.PLAIN, 20) // scalastyle:ignore magic.number

  box setPreferredSize (WindowSize.windowWidth * 0.33, WindowSize.windowHeight * 0.5)
  box setBorder BorderFactory.createEtchedBorder(1)

  private var _assignedTurn: Turn = _
  private val basicMyTurnLabel = "My turn is: "
  private val myTurnLabel = new JLabel(basicMyTurnLabel)
  myTurnLabel setFont labelFont

  box add myTurnLabel

  override def boxType: BoxType = VerticalBox

  def assignedTurn_=(turn: Turn): Unit = {
    _assignedTurn = turn
    myTurnLabel setText _assignedTurn.toString
  }

  def assignedTurn: Turn = _assignedTurn
}