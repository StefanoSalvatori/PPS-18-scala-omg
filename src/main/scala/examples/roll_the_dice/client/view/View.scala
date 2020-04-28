package examples.roll_the_dice.client.view

import examples.roll_the_dice.client.controller.Controller
import examples.roll_the_dice.client.view.scenes.{Loading, Match, MatchSetup, Menu}
import examples.roll_the_dice.common.{MatchState, Team, Turn}
import javax.swing.{JFrame, JPanel}

trait View extends JFrame {

  def observer: Controller

  def startGUI(): Unit

  def closeApplication(): Unit

  def joinGameWithMatchmaking(desiredTeam: Team): Unit

  def leaveMatchmakingQueue(): Unit

  def updateState(newState: MatchState): Unit

  def startGame(assignedTurn: Turn, startingState: MatchState, goalPoints: Int): Unit

  def changeTurn(newTurn: Turn): Unit

  def endGame(winner: Team): Unit

  def showMatchSetup(): Unit
}

object View {
  def apply(observer: Controller): View = new ViewImpl(observer)
}

class ViewImpl(override val observer: Controller) extends View {

  private val _width: Int = WindowSize.windowWidth.intValue
  private val _height: Int = WindowSize.windowHeight.intValue

  this setTitle "Roll The Dice"
  this setSize (_width, _height)
  this setResizable true
  this setDefaultCloseOperation JFrame.EXIT_ON_CLOSE

  private val mainPanel = new JPanel()
  this setContentPane mainPanel

  private val menuScene = Menu(this)
  private val loadingScene = Loading(this)
  private val matchSetupScene = MatchSetup(this)
  private val matchScene = Match(this)

  this changePanel menuScene.panel

  override def startGUI(): Unit = this setVisible true

  override def joinGameWithMatchmaking(desiredTeam: Team): Unit = {
    observer.joinGameWithMatchmaking(desiredTeam)
    this changePanel loadingScene.panel
  }

  override def leaveMatchmakingQueue(): Unit = {
    observer.leaveMatchmakingQueue()
    this changePanel menuScene.panel
  }

  override def closeApplication(): Unit = observer.closeApplication()

  override def updateState(newState: MatchState): Unit = matchScene updateState newState

  override def startGame(assignedTurn: Turn, startingState: MatchState, goalPoints: Int): Unit = {
    matchScene setupGame (assignedTurn, startingState, goalPoints)
    this changePanel matchScene.panel
  }

  override def changeTurn(newTurn: Turn): Unit = matchScene changeTurn newTurn

  override def endGame(winner: Team): Unit = {
    matchScene showWinningDialog winner
    this changePanel menuScene.panel
  }

  override def showMatchSetup(): Unit = this changePanel matchSetupScene.panel

  private def changePanel(newPanel: JPanel): Unit = {
    mainPanel.removeAll()
    mainPanel.repaint()
    mainPanel.revalidate()
    mainPanel add newPanel
  }
}
