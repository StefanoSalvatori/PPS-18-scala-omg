package examples.roll_the_dice.client.view

import examples.roll_the_dice.client.controller.Controller
import examples.roll_the_dice.client.view.scenes.{Loading, Menu}
import javax.swing.{JFrame, JPanel}

trait View extends JFrame {
  def start(): Unit

  def joinGameWithMatchmaking(): Unit

  def leaveMatchmakingQueue(): Unit

  def closeApplication(): Unit
}

object View {
  def apply(observer: Controller): View = new ViewImpl(observer)
}

class ViewImpl(observer: Controller) extends View {

  private val _width: Int = WindowSize.windowWidth.intValue
  private val _height: Int = WindowSize.windowHeight.intValue

  this setTitle "Roll The Dice"
  this setSize (_width, _height)
  this setResizable true

  private val mainPanel = new JPanel()
  this setContentPane mainPanel

  private val menu = Menu(this).panel
  private val loading = Loading(this).panel

  this changePanel menu

  //pack()

  override def start(): Unit = this setVisible true

  override def closeApplication(): Unit = observer.closeApplication()

  override def joinGameWithMatchmaking(): Unit = {
    observer.joinGameWithMatchmaking()
    this changePanel loading
  }

  override def leaveMatchmakingQueue(): Unit = {
    observer.joinGameWithMatchmaking()
    this changePanel menu
  }

  private def changePanel(newPanel: JPanel): Unit = {
    mainPanel.removeAll()
    mainPanel.repaint()
    mainPanel.revalidate()
    mainPanel add newPanel

  }
}
