package examples.rock_paper_scissor.client

import java.io.IOException

import examples.rock_paper_scissor.client.View1.{getClass, stage}
import javafx.{fxml => jfxf}
import javafx.{scene => jfxs}
import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene

object View1 extends JFXApp {


  val root: jfxs.Parent = jfxf.FXMLLoader.load(getClass.getResource("./resources/home.fxml"))

  stage = new PrimaryStage() {
    title = "Client 1"
    scene = new Scene(root)
  }

}

object View2 extends JFXApp {
  val root: jfxs.Parent = jfxf.FXMLLoader.load(getClass.getResource("./resources/home.fxml"))

  stage = new PrimaryStage() {
    title = "Client 2"
    scene = new Scene(root)
  }

}




