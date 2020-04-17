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
  /*
  client.getAvailableRoomsByType("test_room", FilterOptions.empty) onComplete {
    case Success(res) => println("GET: " + res)
  }
  */

  /*
  client.getAvailableRoomsByType("test_room", FilterOptions.empty) onComplete {
    case Success(res) => println(res.head.properties)
  }
  */

  client createPublicRoom "test_room" onComplete {
    case Success(res) =>
      println("POST: " + res.properties + " .. " + res.roomId)
      //println(res.valueOf("CdC"))
  }

  /*
  client createPrivateRoom("test_room", password = "pwd") onComplete {
    case Success(res) =>
      println("POST: " + res.properties)
  }
  */


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
