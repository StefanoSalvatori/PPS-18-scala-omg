package examples.rock_paper_scissor.client

import client.Client
import client.room.ClientRoom
import common.FilterOptions
import javafx.scene.control.{Button, Label}
import javafx.{event => jfxEvent, fxml => jfxf, scene => jfxs}
import scalafx.application.Platform

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class HomeController {
  private val Host = "localhost"
  private val Port = 8080

  @jfxf.FXML var btnNewGame: Button = _
  @jfxf.FXML var btnExit: Button = _
  @jfxf.FXML var labelWaitingPlayer: Label = _
  @jfxf.FXML var  labelStatus: Label = _

  private val client: Client = Client(Host, Port)


  private def goToMatchScene(room: ClientRoom) = {
    Platform.runLater {
      val loader = new jfxf.FXMLLoader(getClass.getResource("./resources/battle.fxml"))
      val root: jfxs.Parent = loader.load()
      loader.getController[BattleController]().init(room)
      this.btnNewGame.getScene.setRoot(root)
    }

  }

  @jfxf.FXML
  def handleNewGameButtonPress(event: jfxEvent.ActionEvent): Unit = {
    this.btnNewGame.setDisable(true)
    this.btnExit.setDisable(true)
    this.labelStatus.setText("joining room...")

    //use client api to join a room. If no one is availabe create one and wait another player
    client.joinOrCreate("match", FilterOptions.empty, Set.empty) onComplete {
      case Success(room) => goToMatchScene(room)
      case Failure(_) => println("client room creation failed")
    }
  }


  @jfxf.FXML
  def handleExitButtonPress(event: jfxEvent.ActionEvent): Unit = {
    Platform.exit()
  }


}
