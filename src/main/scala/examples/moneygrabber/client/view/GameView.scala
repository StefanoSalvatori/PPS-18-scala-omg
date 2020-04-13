package examples.moneygrabber.client.view
import scala.swing._


class GameView(private val worldSize: (Int, Int)) extends MainFrame with Publisher {

  private val TileSize = 20
  title = "MoneyGrabber"
  resizable = false
  val gameGrid: GameGrid = GameGrid(worldSize, TileSize)
  val pointsLabel: Label = new Label {
    text = "points"
  }

  this.contents = new BoxPanel(Orientation.Vertical) {
    contents += pointsLabel
    contents += gameGrid
  }

  def show(): Unit = {
    maximize()
    pack()
    centerOnScreen()
    open()
  }

  def setPlayerPoints(points: List[Int]): Unit = {
    this.pointsLabel.text = "points" + points
  }
}
