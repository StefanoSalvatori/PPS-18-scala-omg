package client.examples

import client.Client
import common.room.RoomPropertyValueConversions._
import common.room.{FilterOptions, RoomProperty}

import scala.concurrent.ExecutionContext
import scala.util.Success

object ClientStub extends App {

  implicit val executionContext: ExecutionContext = ExecutionContext.global
  private val serverAddress = "localhost"
  private val serverPort = 8080

  val client = Client(serverAddress, serverPort)
  val p = Set(RoomProperty("a", 1), RoomProperty("b", "qwe"))
  val filter = FilterOptions.empty

  client createPrivateRoom ("test_room", p, "pwd") onComplete {
    case Success(res) =>
      println("POST: " + res.properties + " ..  " + res.roomId)
  }

  //client.shutdown()
}
