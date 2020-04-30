package examples.rock_paper_scissor.client

import javafx.{fxml => jfxf, scene => jfxs}
import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene

object View1 extends JFXApp {


  private val root: jfxs.Parent = jfxf.FXMLLoader.load(getClass.getResource("./resources/home.fxml"))

  stage = new PrimaryStage() {
    title = "Client 1"
    scene = new Scene(root)
  }

}

object View2 extends JFXApp {
  private val root: jfxs.Parent = jfxf.FXMLLoader.load(getClass.getResource("./resources/home.fxml"))

  stage = new PrimaryStage() {
    title = "Client 2"
    scene = new Scene(root)
  }

}




