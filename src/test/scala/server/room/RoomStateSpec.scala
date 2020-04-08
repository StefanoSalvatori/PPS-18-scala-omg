package server.room

import java.util.UUID

import common.TestConfig
import common.communication.CommunicationProtocol.ProtocolMessageType._
import common.communication.CommunicationProtocol.RoomProtocolMessage
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import server.utils.TestClient

class RoomStateSpec extends AnyWordSpecLike
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll
  with TestConfig {

  private val UpdateRate = 100 //milliseconds
  private val DeltaUpdate = 10 //milliseconds
  private val RoomInitialState: Int = 0

  // Room used for testing
  private case class RoomWithState(override val roomId: String) extends ServerRoom with RoomState[Integer] {
    private var internalState = RoomInitialState
    override val updateRate: Int = UpdateRate

    override def onCreate(): Unit = {}

    override def onClose(): Unit = this.stopStateUpdate()

    override def onJoin(client: Client): Unit = {}

    override def onLeave(client: Client): Unit = {}

    override def onMessageReceived(client: Client, message: Any): Unit = {}

    override def currentState: Integer = this.internalState

    //Only used for testing
    def changeState(newState: Int): Unit = this.internalState = newState
  }


  private var room = RoomWithState(UUID.randomUUID.toString)
  private var client1 = TestClient()
  private var client2 = TestClient()

  before {
    room = RoomWithState(UUID.randomUUID.toString)
    client1 = TestClient()
    client2 = TestClient()
    room.addClient(client1)
    room.addClient(client2)

  }
  after {
    room.close()
  }

  "A room with state" should {
    "not start sending updates before startUpdate() is called" in {
      lastReceivedMessageOf(client1).messageType shouldBe JoinOk
      Thread.sleep(UpdateRate + DeltaUpdate) //wait state update
      lastReceivedMessageOf(client1).messageType shouldBe JoinOk

    }

    "send the room state to clients with a StateUpdate message type" in {
      room.startStateUpdate()
      Thread.sleep(UpdateRate + DeltaUpdate) //wait state update
      lastReceivedMessageOf(client1).messageType shouldBe StateUpdate
    }

    "update the clients with the most recent state" in {
      room.startStateUpdate()
      Thread.sleep(UpdateRate + DeltaUpdate)
      lastReceivedMessageOf(client1) shouldBe RoomProtocolMessage(StateUpdate, client1.id, RoomInitialState)
      val newState = RoomInitialState + 1
      room.changeState(newState)
      Thread.sleep(UpdateRate + DeltaUpdate)
      lastReceivedMessageOf(client1) shouldBe RoomProtocolMessage(StateUpdate, client1.id, newState)
      lastReceivedMessageOf(client2) shouldBe RoomProtocolMessage(StateUpdate, client2.id, newState)

    }

    "wait the update rate before sending new updates" in {
      room.startStateUpdate()
      Thread.sleep(UpdateRate + DeltaUpdate)
      val newState = RoomInitialState + 1
      room.changeState(newState)
      lastReceivedMessageOf(client1) shouldBe RoomProtocolMessage(StateUpdate, client1.id, RoomInitialState)
      lastReceivedMessageOf(client2) shouldBe RoomProtocolMessage(StateUpdate, client2.id, RoomInitialState)
      Thread.sleep(UpdateRate + DeltaUpdate)
      lastReceivedMessageOf(client1) shouldBe RoomProtocolMessage(StateUpdate, client1.id, newState)
      lastReceivedMessageOf(client2) shouldBe RoomProtocolMessage(StateUpdate, client2.id, newState)
    }

    "stop sending the state when stopUpdate is called" in {
      room.startStateUpdate()
      Thread.sleep(UpdateRate + DeltaUpdate)
      lastReceivedMessageOf(client1) shouldBe RoomProtocolMessage(StateUpdate, client1.id, RoomInitialState)
      lastReceivedMessageOf(client2) shouldBe RoomProtocolMessage(StateUpdate, client2.id, RoomInitialState)
      room.stopStateUpdate()
      val newState = RoomInitialState + 1
      room.changeState(newState)
      Thread.sleep(UpdateRate + DeltaUpdate)
      lastReceivedMessageOf(client1) shouldBe RoomProtocolMessage(StateUpdate, client1.id, RoomInitialState)
      lastReceivedMessageOf(client2) shouldBe RoomProtocolMessage(StateUpdate, client2.id, RoomInitialState)

    }

    "restart sending updates when startUpdate is called after stopUpdate" in {
      room.startStateUpdate()
      Thread.sleep(UpdateRate + DeltaUpdate)
      lastReceivedMessageOf(client1) shouldBe RoomProtocolMessage(StateUpdate, client1.id, RoomInitialState)
      lastReceivedMessageOf(client2) shouldBe RoomProtocolMessage(StateUpdate, client2.id, RoomInitialState)
      room.stopStateUpdate()
      val newState = RoomInitialState + 1
      room.changeState(newState)
      Thread.sleep(UpdateRate + DeltaUpdate)
      room.startStateUpdate()
      Thread.sleep(UpdateRate + DeltaUpdate)
      lastReceivedMessageOf(client1) shouldBe RoomProtocolMessage(StateUpdate, client1.id, newState)
      lastReceivedMessageOf(client2) shouldBe RoomProtocolMessage(StateUpdate, client2.id, newState)

    }
  }

  private def lastReceivedMessageOf(client: TestClient): RoomProtocolMessage = {
    client.lastMessageReceived.get.asInstanceOf[RoomProtocolMessage]
  }


}
