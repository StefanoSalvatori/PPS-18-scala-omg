package client.examples

import client.Client
import common.room.BasicRoomPropertyValueConversions._
import common.room.SharedRoom.Room
import common.room.{FilterOptions, RoomProperty}

import scala.concurrent.ExecutionContext
import scala.util.Success

object ClientStub extends App {

  implicit val executionContext: ExecutionContext = ExecutionContext.global
  private val serverAddress = "localhost"
  private val serverPort = 8080

  val client = Client(serverAddress, serverPort)
  val p = Set(RoomProperty("a", 1), RoomProperty("b", "qwe"), RoomProperty("c", true), RoomProperty("df", 1))
  /*
  client.getAvailableRoomsByType("test_room", FilterOptions.empty) onComplete {
    case Success(res) => println("GET: " + res)
  }
  */

  client createPrivateRoom("test_room", Set.empty[RoomProperty], "abc") onComplete {
    case Success(res) => println("POST: " + res)
  }
  /*
  andThen {
    case Success(_) =>
      client.getAvailableRoomsByType("test_room", FilterOptions.empty) onComplete {
        case Success(rooms) => println("GET: " + rooms)
      }
  }
  */


  //client.shutdown()
}
