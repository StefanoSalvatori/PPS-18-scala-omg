package examples.rock_paper_scissor.client

import client.room.ClientRoom
import javafx.scene.control.Label
import javafx.scene.layout.GridPane
import javafx.{event => jfxEvent, fxml => jfxf}
import scalafx.application.Platform
import javafx.{fxml => jfxf}
import javafx.{scene => jfxs}


class MatchController {
  @jfxf.FXML var labelWaitingPlayer: Label = _
  @jfxf.FXML var gridPaneButtons: GridPane = _
  private var room: ClientRoom = _

  def init(room: ClientRoom): Unit = {
    this.room = room
    room.onMessageReceived {
      //ready to play
      case "start" =>
        Platform.runLater {
          this.labelWaitingPlayer.setText("Play!")
          this.gridPaneButtons.setVisible(true)
        }

      //game result: win, lose or draw
      case msg =>
        Platform.runLater {
          val loader = new jfxf.FXMLLoader(getClass.getResource("./resources/game_end.fxml"))
          val root: jfxs.Parent = loader.load()
          loader.getController[GameEndController]().init(msg)
          this.gridPaneButtons.getScene.setRoot(root)
        }

    }

  }

  @jfxf.FXML
  def handleRockButtonPress(event: jfxEvent.ActionEvent): Unit = {
    makeMove("rock")
  }

  @jfxf.FXML
  def handlePaperButtonPress(event: jfxEvent.ActionEvent): Unit = {
    makeMove("paper")
  }

  @jfxf.FXML
  def handleScissorButtonPress(event: jfxEvent.ActionEvent): Unit = {
    makeMove("scissor")
  }

  private def makeMove(move: String): Unit = {
    room.send(move)
    Platform.runLater {
      this.labelWaitingPlayer.setText("You played -> " + move)
      this.gridPaneButtons.setDisable(true)
    }
  }


}



