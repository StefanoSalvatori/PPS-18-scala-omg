package client

import common.FilterOptions

import scala.concurrent.ExecutionContext
import scala.util.Success
object ClientStub extends App {

  implicit val executionContext: ExecutionContext = ExecutionContext.global
  private val serverAddress = "localhost"
  private val serverPort = 8080

  val client = Client(serverAddress, serverPort)
  client createPublicRoom("test_room", Set.empty) andThen {
    case Success(_) =>
      client.getAvailableRoomsByType("test_room", FilterOptions.empty()) onComplete {
        case Success(rooms) => println(rooms)
      }
  } onComplete {
    case Success(res) => println(res)
  }

  //client.shutdown()
}
