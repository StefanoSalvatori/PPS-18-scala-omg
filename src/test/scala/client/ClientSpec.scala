package client

import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ClientSpec extends AnyFlatSpec
  with Matchers
  with BeforeAndAfter {

  private val serverAddress = "localhost"
  private val serverPort = 8080
  private var client: Client = _

  behavior of "Client"

  before {
    client = Client(serverAddress, serverPort)
  }

  it should "start with no joined rooms" in {
    assert(client.joinedRooms().isEmpty)
  }

  it should "create a public room and automatically join such room" in {
    client.createPublicRoom()
    Thread sleep 3000
    client.joinedRooms().size shouldEqual 1
  }
}
