package client

import common.{FilterOptions, RoomProperty}
import common.BasicRoomPropertyValueConversions._

import scala.concurrent.ExecutionContext
import scala.util.Success
object ClientStub extends App {

  implicit val executionContext: ExecutionContext = ExecutionContext.global
  private val serverAddress = "localhost"
  private val serverPort = 8080

  val client = Client(serverAddress, serverPort)
  val p = Set(RoomProperty("a", 1), RoomProperty("b", "qwe"), RoomProperty("c", true), RoomProperty("df", 1))
  client createPublicRoom("prova", p)  onComplete {
    case Success(res) => println("POST: " + res)
  }
  /*andThen {
    case Success(_) =>
      client.getAvailableRoomsByType("test_room", FilterOptions.empty) onComplete {
        case Success(rooms) => println("GET: " + rooms)
      }
  }*/


  //client.shutdown()
}
