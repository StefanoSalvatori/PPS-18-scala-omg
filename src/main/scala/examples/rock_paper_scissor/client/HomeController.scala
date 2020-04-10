package examples.rock_paper_scissor.client

import client.Client
import client.room.ClientRoom
import common.room.FilterOptions
import javafx.scene.control.{Button, Label}
import javafx.scene.layout.VBox
import javafx.{event => jfxEvent, fxml => jfxf, scene => jfxs}
import scalafx.application.Platform

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class HomeController {
  private val Host = "localhost"
  private val Port = 8080

  @jfxf.FXML private var btnNewGame: Button = _
  @jfxf.FXML private var btnExit: Button = _
  @jfxf.FXML private var labelWaitingPlayer: Label = _
  @jfxf.FXML private var labelStatus: Label = _
  @jfxf.FXML private var vboxMenuButtons: VBox = _

  private val client: Client = Client(Host, Port)


  private def goToMatchScene(room: ClientRoom, gameMode: String) = {
    Platform.runLater {
      val loader = new jfxf.FXMLLoader(getClass.getResource("./resources/match.fxml"))
      val root: jfxs.Parent = loader.load()
      loader.getController[MatchController]().init(room, gameMode)
      this.btnNewGame.getScene.setRoot(root)
    }

  }

  @jfxf.FXML
  def handleClassicGameButtonPress(event: jfxEvent.ActionEvent): Unit = {
    this.vboxMenuButtons.setDisable(true)
    this.labelStatus.setText("joining classic room...")

    //use client api to join a room. If no one is available create one and wait another player
    client.joinOrCreate("classic", FilterOptions.empty) onComplete {
      case Success(room) => goToMatchScene(room, "classic")
      case Failure(_) => println("client room creation failed")
    }
  }

  @jfxf.FXML
  def handleAdvancedGameButtonPress(event: jfxEvent.ActionEvent): Unit = {
    this.vboxMenuButtons.setDisable(true)

    this.labelStatus.setText("joining advanced room...")

    //use client api to join a room. If no one is availabe create one and wait another player
    client.joinOrCreate("advanced", FilterOptions.empty, Set.empty) onComplete {
      case Success(room) => goToMatchScene(room, "advanced")
      case Failure(_) => println("client room creation failed")
    }
  }


  @jfxf.FXML
  def handleExitButtonPress(event: jfxEvent.ActionEvent): Unit = {
    Platform.exit()
  }


}
