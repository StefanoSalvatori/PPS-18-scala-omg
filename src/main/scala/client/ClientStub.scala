package client

import common.actors.ApplicationActorSystem

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}
object ClientStub extends App {

  import common.actors.ApplicationActorSystem._
  private val serverAddress = "localhost"
  private val serverPort = 8080

  val client = Client(serverAddress, serverPort)
  client createPublicRoom ("test_room", "") andThen {
    case Success(_) =>
      client.getAvailableRoomsByType("test_room") onComplete {
        case Success(rooms) => println(rooms)
      }
  } onComplete {
    case Success(res) => println(res)
  }

  //client.shutdown()
}
