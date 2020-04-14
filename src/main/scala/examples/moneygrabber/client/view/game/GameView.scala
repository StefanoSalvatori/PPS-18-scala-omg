package examples.moneygrabber.client.view.game

import java.awt.Color

import examples.moneygrabber.client.view.game.GameGrid.ButtonPressedEvent

import scala.swing._
import scala.swing.event.KeyPressed


class GameView(private val worldSize: (Int, Int), numPlayers: Int) extends BoxPanel(Orientation.Vertical) {
  private val TileSize = 20
  private val PlayerInfoColorSize = 10
  private val CoinColor = Color.yellow
  val PlayerIdToColor: PartialFunction[Int, Color] = {
    case 0 => Color.red
    case 1 => Color.blue
    case 2 => Color.green
    case 3 => Color.orange
    case _ => Color.white
  }
  focusable = true
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
  private val topPanel: Panel = new GridPanel(1, 2) {
    contents += playerInfo
    contents += pointsInfo
  }
  private val gameGrid: GameGrid = GameGrid(worldSize, TileSize)

  contents += topPanel
  contents += gameGrid

  listenTo(keys, gameGrid.keys)
  reactions += {
    case event: KeyPressed => publish(ButtonPressedEvent(event.key))
  }

  def clearTiles(): Unit = {
    this.gameGrid.resetGrid()
  }

  def colorCoinTile(tile: (Int, Int)): Unit = {
    this.gameGrid.colorButton(tile, CoinColor)
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
