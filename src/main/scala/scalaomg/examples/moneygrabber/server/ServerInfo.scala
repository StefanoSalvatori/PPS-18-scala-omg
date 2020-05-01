package scalaomg.examples.moneygrabber.server

object ServerInfo {
  val DefaultHost = "localhost"
  val DefaultPort = 8080
}
case class ServerInfo(host: String, port: Int)
