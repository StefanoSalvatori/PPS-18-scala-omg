package examples.roll_the_dice.client.view.scenes

import java.awt.{BorderLayout, Color, Font}

import examples.roll_the_dice.client.view.{View, WindowSize}
import examples.roll_the_dice.common.{A1, A2, B1, B2, MatchState, Team, Turn}
import javax.swing.{BorderFactory, Box, JButton, JLabel, JOptionPane}
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
  private val middleBox = MiddleBox(view)
  panel add (middleBox.box, BorderLayout.CENTER)

  def updateState(newState: MatchState): Unit = pointsBox updatePoints newState

  def setupGame(assignedTurn: Turn, startingState: MatchState, goalPoints: Int): Unit = {
    pointsBox setupGame (startingState, goalPoints)
    middleBox.assignedTurn = assignedTurn
  }

  def changeTurn(newTurn: Turn): Unit = middleBox.currentTurn = newTurn

  def showWinningDialog(team: Team): Unit = JOptionPane.showMessageDialog(
    view,
    s"Team $team wins!",
    "Game end",
    JOptionPane.INFORMATION_MESSAGE
  )

}

private case class PointsBox() extends BasicBox {

  private val spacingDimension = (20, 20)

  private var goalPoints: Int = 0
  private var currentState = MatchState()

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
  box setPreferredSize (WindowSize.windowWidth * 0.5, WindowSize.windowHeight * 0.3)

  box add Box.createRigidArea(spacingDimension)
  box add redTeamLabel
  box add Box.createRigidArea(spacingDimension)
  box add blueTeamLabel

  override def boxType: BoxType = VerticalBox

  def updatePoints(newState: MatchState): Unit = {
    currentState = newState
    redTeamLabel setText redTeamName + " " + newState.pointsA + " / " + goalPoints
    blueTeamLabel setText blueTeamName + " " + newState.pointsB + " / " + goalPoints
  }

  def setupGame(startingState: MatchState, goalPoints: Int): Unit = {
    this.goalPoints = goalPoints
    currentState = startingState
    this updatePoints currentState
  }
}

private case class MiddleBox(private val view: View) extends BasicBox {

  private val spacingDimension = (20, 20) // scalastyle:ignore magic.number

  private val labelFont = new Font(new JLabel().getName, Font.PLAIN, 20) // scalastyle:ignore magic.number

  box setPreferredSize (WindowSize.windowWidth * 0.33, WindowSize.windowHeight * 0.5)
  box setBorder BorderFactory.createEtchedBorder(1)

  // My turn
  private var _assignedTurn: Turn = _
  private val basicMyTurnLabel = "My turn is: "
  private val myTurnLabel = new JLabel(basicMyTurnLabel)
  myTurnLabel setFont labelFont

  // Current turn
  private var _currentTurn: Turn = _
  private val basicCurrentTurnLabel = "Current turn: "
  private val currentTurnLabel = new JLabel(basicCurrentTurnLabel)
  currentTurnLabel setFont labelFont

  // Roll
  private val rollButton = new JButton("Roll")
  rollButton addActionListener(_ => view.observer.rollDice())

  box add Box.createRigidArea(spacingDimension)
  box add myTurnLabel
  box add Box.createRigidArea(spacingDimension)
  box add currentTurnLabel
  box add Box.createRigidArea(spacingDimension)
  box add rollButton

  override def boxType: BoxType = VerticalBox

  def assignedTurn: Turn = _assignedTurn

  def assignedTurn_=(turn: Turn): Unit = {
    _assignedTurn = turn
    myTurnLabel setText basicMyTurnLabel + _assignedTurn.toString
  }

  def currentTurn: Turn = _currentTurn

  def currentTurn_=(turn: Turn): Unit = {
    _currentTurn = turn
    currentTurnLabel setText basicCurrentTurnLabel + _currentTurn.toString
    if (_currentTurn == _assignedTurn) {
      rollButton setEnabled true
    } else {
      rollButton setEnabled false
    }
  }
}

private case class TeamToolbar(player1: Turn, player2: Turn) extends BasicBox {

  private val spacingDimension = (20, 20) // scalastyle:ignore magic.number

  private val player1Box = PlayerBox(player1)
  private val player2Box = PlayerBox(player2)

  box add Box.createRigidArea(spacingDimension)
  box add player1Box.box
  box add Box.createRigidArea(spacingDimension)
  box add player2Box.box
  box add Box.createRigidArea(spacingDimension)

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