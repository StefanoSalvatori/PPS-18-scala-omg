package client

object ClientStub extends App {

  private val serverAddress = "localhost"
  private val serverPort = 8080
  val client = Client(serverAddress, serverPort)

  client createPublicRoom "test_room"

  //client.shutdown()
}
