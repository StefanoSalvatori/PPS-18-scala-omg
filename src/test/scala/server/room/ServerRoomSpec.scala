package server.room

import java.util.UUID

import common.room.SharedRoom.RoomId
import common.communication.CommunicationProtocol.ProtocolMessageType._
import common.communication.CommunicationProtocol.RoomProtocolMessage
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import common.room.BasicRoomPropertyValueConversions._
import common.room.{RoomProperty, RoomPropertyValue}
import server.utils.TestClient

class ServerRoomSpec extends AnyWordSpecLike
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll {

  private val serverRoom = ServerRoom()
  private val testClient = TestClient(UUID.randomUUID().toString)
  private val testClient2 = TestClient(UUID.randomUUID().toString)

  val numOfProperties = 5 // A, B, C, D + roomId
  val nameA = "a"; val valueA = 1
  val nameB = "b"; val valueB = "abc"
  val nameC = "c"; val valueC = false
  val nameD = "d"; val valueD = 0.1

  var testRoom: ServerRoom = _

  before {
    testRoom = new ServerRoom {
      override val roomId: RoomId = "id"
      var a: Int = valueA
      var b: String = valueB
      var c: Boolean = valueC
      var d: Double = valueD

      override def onCreate(): Unit = {}
      override def onClose(): Unit = {}
      override def onJoin(client: Client): Unit = {}
      override def onLeave(client: Client): Unit = {}
      override def onMessageReceived(client: Client, message: Any): Unit = {}
    }
  }

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
      val received = testClient.lastMessageReceived.get.asInstanceOf[RoomProtocolMessage]
      received.messageType shouldBe Tell
      received.payload shouldBe "Hello"
    }

    "send broadcast messages to all clients connected using the room protocol" in {
      serverRoom.addClient(testClient2)
      serverRoom.broadcast("Hello Everybody")
      val receivedFrom1 = testClient.lastMessageReceived.get.asInstanceOf[RoomProtocolMessage]
      receivedFrom1.messageType shouldBe Broadcast
      receivedFrom1.payload shouldBe "Hello Everybody"
      val receivedFrom2 = testClient.lastMessageReceived.get.asInstanceOf[RoomProtocolMessage]
      receivedFrom2.messageType shouldBe Broadcast
      receivedFrom2.payload shouldBe "Hello Everybody"
    }

    "return correct values of its properties" in {
      testRoom valueOf nameA shouldEqual valueA
      testRoom valueOf nameB shouldEqual valueB
      testRoom valueOf nameC shouldEqual valueC
      testRoom valueOf nameD shouldEqual valueD
    }

    "return its room property value when required" in {
      assert((testRoom `valueOf~AsProperty` nameA).isInstanceOf[RoomPropertyValue])
    }

    "return the associated room property, given a property name" in {
      testRoom propertyOf nameA shouldEqual RoomProperty(nameA, valueA)
      testRoom propertyOf nameB shouldEqual RoomProperty(nameB, valueB)
      testRoom propertyOf nameC shouldEqual RoomProperty(nameC, valueC)
      testRoom propertyOf nameD shouldEqual RoomProperty(nameD, valueD)
    }

    "not be updated when using an empty set of properties" in {
      val p = Set.empty[RoomProperty]
      testRoom setProperties p
      testRoom valueOf nameA shouldEqual valueA
      testRoom valueOf nameB shouldEqual valueB
      testRoom valueOf nameC shouldEqual valueC
      testRoom valueOf nameD shouldEqual valueD
    }

    "be correctly updated" in {
      val p = Set(RoomProperty(nameA, 1), RoomProperty(nameB, "qwe"))
      testRoom setProperties p
      testRoom valueOf nameA shouldEqual 1
      testRoom valueOf nameB shouldEqual "qwe"
    }

    "notify the error when trying to read a non existing property" in {
      assertThrows[NoSuchFieldException] {
        testRoom valueOf "randomName"
      }
    }

    "be safely handled when trying to write a non existing property" in {
      testRoom setProperties Set(RoomProperty("randomName", 0))
      noException
    }

    "be public by default" in {
      assert(!testRoom.isPrivate)
    }

    "become private when setting a password" in {
      val password = "pwd"
      testRoom makePrivate password
      assert(testRoom.isPrivate)
    }

    "become public when required" in {
      val password = "pwd"
      testRoom makePrivate password
      testRoom.makePublic()
      assert(!testRoom.isPrivate)
    }

    "expose the defined room properties" in {
      assert(testRoom.properties contains RoomProperty(nameA, valueA))
      assert(testRoom.properties contains RoomProperty(nameB, valueB))
      assert(testRoom.properties contains RoomProperty(nameC, valueC))
      assert(testRoom.properties contains RoomProperty(nameD, valueD))
    }

    "expose just the correct properties" in {
      val roomProperties = testRoom.properties
      val parentProperties = ServerRoom.defaultProperties
      roomProperties &~ parentProperties should have size numOfProperties
      assert(roomProperties contains RoomProperty(nameA, valueA))
      assert(roomProperties contains RoomProperty(nameB, valueB))
      assert(roomProperties contains RoomProperty(nameC, valueC))
      assert(roomProperties contains RoomProperty(nameD, valueD))
    }

  }

}
