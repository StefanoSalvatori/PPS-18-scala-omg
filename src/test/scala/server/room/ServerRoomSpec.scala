package server.room

import java.util.UUID

import common.communication.CommunicationProtocol.{Broadcast, RoomProtocolMessage, Tell}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}

class ServerRoomSpec extends AnyWordSpecLike
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll {

  /**
   * A client that expose the last message received.
   */
  class TestableClient(override val id: String) extends Client {
    var lastMessagedReceived: Option[Any] = Option.empty

    override def send[T](msg: T): Unit = this.lastMessagedReceived = Option(msg)
  }


  private val serverRoom = ServerRoom(UUID.randomUUID().toString)
  private val testClient = new TestableClient(UUID.randomUUID().toString)
  private val testClient2 = new TestableClient(UUID.randomUUID().toString)


  "A server room" should {
    "start with no clients connected" in {
      assert(serverRoom.connectedClients.isEmpty)
    }

    "add clients to the room" in {
      serverRoom.addClient(testClient)
      serverRoom.addClient(testClient2)
      assert(serverRoom.connectedClients.contains(testClient2))
      assert(serverRoom.connectedClients.contains(testClient))

    }

    "remove clients from the room" in {
      serverRoom.removeClient(testClient2)
      serverRoom.removeClient(testClient)
      assert(serverRoom.connectedClients.isEmpty)
    }

    "send specific messages to clients using the room protocol" in {
      serverRoom.addClient(testClient)
      serverRoom.tell(testClient, "Hello")
      testClient.lastMessagedReceived shouldBe Option(RoomProtocolMessage(Tell, "Hello"))
    }

    "send broadcast messages to all clients connected using the room protocol" in {
      serverRoom.addClient(testClient2)
      serverRoom.broadcast("Hello Everybody")
      testClient.lastMessagedReceived shouldBe Option(RoomProtocolMessage(Broadcast, "Hello Everybody"))
      testClient2.lastMessagedReceived shouldBe Option(RoomProtocolMessage(Broadcast, "Hello Everybody"))
    }

  }

}
