package scalaomg.examples.moneygrabber.client.view.game

import java.awt.Color

import scalaomg.examples.moneygrabber.client.view.game.GameGrid.ButtonPressedEvent
import scalaomg.examples.moneygrabber.client.view.game.GameView.{GameStatus, WaitingPlayers}
import scalaomg.examples.moneygrabber.common.Board

import scala.swing.GridBagPanel.Anchor
import scala.swing._
import scala.swing.event.KeyPressed

object GameView {
  class GameStatus(val text: String)
  case object WaitingPlayers extends GameStatus(s"Waiting players...")
  case object GameStarted extends GameStatus("Game started!")
  case object GameEnd extends GameStatus("Game ended")
}


class GameView(private val worldSize: (Int, Int), numPlayers: Int) extends BoxPanel(Orientation.Vertical) {
  private val TileSize = 20
  private val PlayerInfoColorSize = 10

  private def CoinColor(value: Int) = value match {
    case Board.CoinBasicValue => Color.yellow
    case Board.HunterDropValue => Color.white
  }

  private val HunterColor = Color.black
  val PlayerIdToColor: PartialFunction[Int, Color] = {
    case 0 => Color.red
    case 1 => Color.blue
    case 2 => Color.green
    case 3 => Color.pink
    case _ => Color.white
  }
  focusable = true
  private val gameStatus: Label = new Label {
    text = WaitingPlayers.text
  }
  private val playerInfo: FlowPanel = new FlowPanel {
    contents += new Label("You are:")
    contents += new Button {
      background = Color.GRAY
      preferredSize = new Dimension(PlayerInfoColorSize, PlayerInfoColorSize)
    }
  }
  private val pointsInfo: GridPanel = new GridPanel(1, numPlayers) {
    for (id <- 0 until numPlayers) {
      contents += new PointsInfo(id)
    }
  }
  private val topPanel: Panel = new GridBagPanel {
    val c1 = new Constraints
    c1.grid = (0, 0); c1.weightx = 0.5
    layout(gameStatus) = c1
    c1.grid = (1, 0)
    layout(playerInfo) = c1
    val c2 = new Constraints
    c2.gridy = 1
    c2.gridwidth = 3
    c2.anchor = Anchor.Center
    c2.weightx = 0.5
    layout(pointsInfo) = c2

  }
  private val gameGrid: GameGrid = GameGrid(worldSize, TileSize * TileSize / worldSize._1)

  contents += topPanel
  contents += gameGrid

  listenTo(keys, gameGrid.keys)
  reactions += {
    case event: KeyPressed => publish(ButtonPressedEvent(event.key))
  }

  def clearTiles(): Unit = {
    this.gameGrid.resetGrid()
  }

  def colorHunterTile(position: (Int, Int)): Unit = {
    this.gameGrid.colorButton(position, HunterColor)
  }

  def colorCoinTile(tile: (Int, Int), value: Int): Unit = {
    this.gameGrid.colorButton(tile, CoinColor(value))
  }

  def colorPlayerTile(playerId: Int, tile: (Int, Int)): Unit = {
    this.gameGrid.colorButton(tile, PlayerIdToColor(playerId))
  }

  def showPlayersPoints(players: Map[Int, Int]): Unit = {
    players.foreach(entry => {
      this.pointsInfo.contents.map(_.asInstanceOf[PointsInfo]).filter(_.playerId == entry._1)
        .foreach(panel => panel.playerPoints.text = players(panel.playerId).toString)
    })
  }

  def setPlayerColor(playerId: Int): Unit = {
    this.playerInfo.contents.find(_.isInstanceOf[Button]).foreach(_.background = PlayerIdToColor(playerId))
  }

  def setGameStatus(state: GameStatus): Unit = {
    this.gameStatus.text = state.text
  }

  private class PointsInfo(val playerId: Int) extends FlowPanel {
    private val PlayerColorSize = 10
    val playerColor: Button = new Button {
      background = PlayerIdToColor(playerId)
      preferredSize = new Dimension(PlayerColorSize, PlayerColorSize)
    }
    val playerPoints: Label = new Label("0")
    contents += playerColor
    contents += playerPoints

  }
}
