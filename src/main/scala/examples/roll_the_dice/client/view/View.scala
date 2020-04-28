package examples.roll_the_dice.client.view

import examples.roll_the_dice.client.controller.Controller
import examples.roll_the_dice.client.view.scenes.{Loading, Match, Menu}
import examples.roll_the_dice.common.{MatchState, Team, Turn}
import javax.swing.{JFrame, JPanel}

trait View extends JFrame {

  def observer: Controller

  def start(): Unit

  def joinGameWithMatchmaking(): Unit

  def leaveMatchmakingQueue(): Unit

  def closeApplication(): Unit

  def updateState(newState: MatchState): Unit

  def startGame(assignedTurn: Turn, startingState: MatchState, goalPoints: Int): Unit

  def changeTurn(newTurn: Turn): Unit

  def endGame(winner: Team): Unit
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
  private val matchScene = Match(this)

  this changePanel menuScene.panel
//  this changePanel matchScene.panel

  //pack()

  override def start(): Unit = this setVisible true

  override def joinGameWithMatchmaking(): Unit = {
    observer.joinGameWithMatchmaking()
    this changePanel loadingScene.panel
  }

  override def leaveMatchmakingQueue(): Unit = {
    observer.joinGameWithMatchmaking()
    this changePanel menuScene.panel
  }

  override def closeApplication(): Unit = observer.closeApplication()

  override def updateState(newState: MatchState): Unit = matchScene updateState newState

  private def changePanel(newPanel: JPanel): Unit = {
    mainPanel.removeAll()
    mainPanel.repaint()
    mainPanel.revalidate()
    mainPanel add newPanel
  }

  override def startGame(assignedTurn: Turn, startingState: MatchState, goalPoints: Int): Unit = {
    matchScene setupGame (assignedTurn, startingState, goalPoints)
    this changePanel matchScene.panel
  }

  override def changeTurn(newTurn: Turn): Unit = matchScene changeTurn newTurn

  override def endGame(winner: Team): Unit = {
    println(s"$winner won")
  }
}
