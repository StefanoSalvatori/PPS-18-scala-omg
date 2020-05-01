package scalaomg.examples.rock_paper_scissor.client

import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.Includes._
import javafx.{fxml => jfxf, scene => jfxs}

object App extends JFXApp {
  private val DefaultPort = 8080
  private val args: Array[String] = parameters.raw.toArray
  private val host = if (args.length > 0) args(0) else "localhost"
  private val port = if (args.length > 1) args(1).toInt else DefaultPort

  val root: jfxs.Parent = new jfxf.FXMLLoader(getClass.getResource("./resources/home.fxml")).load()
  Model.init(host, port)
  println(s"Client started at $host:$port")

  stage = new PrimaryStage {
    title = "Rock Paper Scissor"
    scene = new Scene(root)
  }
}
