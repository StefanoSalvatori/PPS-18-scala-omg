package server.room

import akka.actor.{ActorRef, ActorSystem}
import common.TestConfig
import common.communication.CommunicationProtocol.ProtocolMessageType._
import common.communication.CommunicationProtocol.RoomProtocolMessage
import common.room.Room
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import server.RoomHandler
import server.utils.TestClient

class SynchronizedRoomStateSpec extends AnyWordSpecLike
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll
  with TestConfig
  with Eventually {

  import scala.concurrent.duration._
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(20 seconds, 25 millis)
  implicit private val actorSystem: ActorSystem = ActorSystem()

  import server.utils.ExampleRooms.RoomWithState
  import server.utils.ExampleRooms.RoomWithState._
  private var room: RoomWithState = _
  private var roomActor: ActorRef = _
  private var client1 = TestClient()
  private var client2 = TestClient()

  before {
    // Can't directly use roomHandler.createRoom since we need server room type instance
    room = RoomWithState()
    roomActor = actorSystem actorOf RoomActor(room, RoomHandler())
    client1 = TestClient()
    client2 = TestClient()
    room.tryAddClient(client1, Room.defaultPublicPassword)
    room.tryAddClient(client2, Room.defaultPublicPassword)

  }
  after {
    room.close()
  }

  "A room with state" should {
    "not start sending updates before startUpdate() is called" in {
      lastReceivedMessageOf(client1).messageType shouldBe JoinOk
      Thread.sleep(UpdateRate) //wait state update
      lastReceivedMessageOf(client1).messageType shouldBe JoinOk

    }

    "send the room state to clients with a StateUpdate message type" in {
      room.startStateUpdate()
      eventually {
        lastReceivedMessageOf(client1).messageType shouldBe StateUpdate
      }
    }

    "update the clients with the most recent state" in {
      room.startStateUpdate()
      eventually {
        lastReceivedMessageOf(client1) shouldBe RoomProtocolMessage(StateUpdate, client1.id, RoomInitialState)
      }
      val newState = RoomInitialState + 1
      room.changeState(newState)
      eventually {
        lastReceivedMessageOf(client1) shouldBe RoomProtocolMessage(StateUpdate, client1.id, newState)
        lastReceivedMessageOf(client2) shouldBe RoomProtocolMessage(StateUpdate, client2.id, newState)
      }

    }

    "stop sending the state when stopUpdate is called" in {
      room.startStateUpdate()
      eventually {
        lastReceivedMessageOf(client1) shouldBe RoomProtocolMessage(StateUpdate, client1.id, RoomInitialState)
        lastReceivedMessageOf(client2) shouldBe RoomProtocolMessage(StateUpdate, client2.id, RoomInitialState)
      }
      room.stopStateUpdate()
      val newState = RoomInitialState + 1
      room.changeState(newState)
      Thread.sleep(UpdateRate)
      lastReceivedMessageOf(client1) shouldBe RoomProtocolMessage(StateUpdate, client1.id, RoomInitialState)
      lastReceivedMessageOf(client2) shouldBe RoomProtocolMessage(StateUpdate, client2.id, RoomInitialState)

    }

    "restart sending updates when startUpdate is called after stopUpdate" in {
      room.startStateUpdate()
      eventually {
        lastReceivedMessageOf(client1) shouldBe RoomProtocolMessage(StateUpdate, client1.id, RoomInitialState)
        lastReceivedMessageOf(client2) shouldBe RoomProtocolMessage(StateUpdate, client2.id, RoomInitialState)
      }
      room.stopStateUpdate()
      val newState = RoomInitialState + 1
      room.changeState(newState)
      Thread.sleep(UpdateRate)
      room.startStateUpdate()
      eventually {
        lastReceivedMessageOf(client1) shouldBe RoomProtocolMessage(StateUpdate, client1.id, newState)
        lastReceivedMessageOf(client2) shouldBe RoomProtocolMessage(StateUpdate, client2.id, newState)
      }

    }
  }

  private def lastReceivedMessageOf(client: TestClient): RoomProtocolMessage = {
    client.lastMessageReceived.get.asInstanceOf[RoomProtocolMessage]
  }


}
