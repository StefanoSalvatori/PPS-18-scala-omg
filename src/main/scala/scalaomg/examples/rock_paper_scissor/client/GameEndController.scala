package scalaomg.examples.rock_paper_scissor.client

import javafx.scene.control.Label
import javafx.{event => jfxEvent, fxml => jfxf, scene => jfxs}
import scalafx.application.Platform

class GameEndController {
  @jfxf.FXML private var labelGameResult: Label = _

  def init(gameState: String): Unit = {
    gameState match {
      case "win" =>
        Platform.runLater { labelGameResult.setText("You Win!") }
      case "lose" =>
        Platform.runLater { labelGameResult.setText("You Lose!") }
      case "draw" =>
        Platform.runLater { labelGameResult.setText("Draw!") }
    }
  }

  @jfxf.FXML
  def handleHomeButtonPress(event: jfxEvent.ActionEvent): Unit = {
    val loader = new jfxf.FXMLLoader(getClass.getResource("./resources/home.fxml"))
    val root: jfxs.Parent = loader.load()
    this.labelGameResult.getScene.setRoot(root)
  }



}
