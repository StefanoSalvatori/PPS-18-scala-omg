package scalaomg.examples.roll_the_dice.common

object ServerInfo {
  val DefaultHost = "localhost"
  val DefaultPort = 8080
}
case class ServerInfo(host: String, port: Int)
