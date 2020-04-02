package client

import common.actors.ApplicationActorSystem

import scala.util.{Failure, Success}
object ClientStub extends App with ApplicationActorSystem{

  private val serverAddress = "localhost"
  private val serverPort = 8080

  val client = Client(serverAddress, serverPort)
  client createPublicRoom("test_room", "") onComplete {
    case Success(room) => println("Room id ->" + room.roomId)
    case Failure(exception) => println("Fail " + exception)
  }

  //client.shutdown()
}
