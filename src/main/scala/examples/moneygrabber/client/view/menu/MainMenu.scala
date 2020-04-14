package examples.moneygrabber.client.view.menu

import java.awt.{Color, Insets}

import javax.swing.BorderFactory

import scala.swing.GridBagPanel.Fill
import scala.swing.event.Event
import scala.swing.{Button, MainFrame, _}

object MainMenu {
  sealed trait MenuButtonClicked extends Event
  case object TwoPlayers extends MenuButtonClicked
  case object FourPlayers extends MenuButtonClicked
  case object Quit extends MenuButtonClicked

  val MenuButtons = List(
    MenuButton("2 Players", TwoPlayers),
    MenuButton("4 Players", FourPlayers),
    MenuButton("Quit", Quit)
  )

  case class MenuButton(buttonText: String, buttonType: MenuButtonClicked) extends Button {
    val MaxWidth = 175
    val MaxHeight = 75
    val FontSize = 15
    text = buttonText
    font = Font(Font.Monospaced, Font.Style.Bold, FontSize)
    background = Color.white
    preferredSize = new Dimension(MaxWidth, MaxHeight)
    maximumSize = new Dimension(MaxWidth, MaxHeight)
    reactions += {
      case event.ButtonClicked(_) => publish(buttonType)
    }
  }
}

class MainMenu extends MainFrame {
  private val TitleMargin = 20
  private val MenuButtonHGap = 20
  private val MenuButtonsTopDownMargin = 15
  private val MenuButtonsLeftRightMargin = 45
  private val TitleFontSize = 24
  private val TitleFont = Font(Font.Monospaced, Font.Style.Bold, TitleFontSize)
  private val FrameWidth = 350
  private val FrameHeight = 400
  private val DefaultWindowSize = new Dimension(FrameWidth, FrameHeight)

  title = "Money Grabber"
  preferredSize = DefaultWindowSize
  contents = new BorderPanel {
    border = BorderFactory.createEmptyBorder(TitleMargin, TitleMargin, TitleMargin, TitleMargin)
    val menuTitle: Label = new Label("Money Grabber") {
      foreground = Color.black
      font = TitleFont
      horizontalAlignment = Alignment.Center
    }

    val menuButtons: GridBagPanel = new GridBagPanel {
      val c = new Constraints
      c.insets = new Insets(MenuButtonHGap, MenuButtonHGap, MenuButtonHGap, MenuButtonHGap)
      border = BorderFactory.createEmptyBorder(
        MenuButtonsTopDownMargin, MenuButtonsLeftRightMargin,
        MenuButtonsTopDownMargin, MenuButtonsLeftRightMargin)
      c.fill = Fill.Horizontal
      MainMenu.MenuButtons.zipWithIndex.foreach(b => {
        c.grid = (0, b._2)
        layout(b._1) = c
      })
    }
    layout(menuTitle) = BorderPanel.Position.North
    layout(menuButtons) = BorderPanel.Position.Center
  }
}
