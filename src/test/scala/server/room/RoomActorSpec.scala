package server.room

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import server.room.RoomActor._
import common.communication.CommunicationProtocol.ProtocolMessageType._
import common.room.Room
import server.RoomHandler
class RoomActorSpec extends TestKit(ActorSystem("Rooms", ConfigFactory.load()))
  with ImplicitSender
  with Matchers
  with AnyWordSpecLike
  with BeforeAndAfter
  with BeforeAndAfterAll {


  private val client1TestProbe = TestProbe()
  private val FAKE_CLIENT_1 = Client.asActor(UUID.randomUUID.toString, client1TestProbe.ref)

  private val client2TestProbe = TestProbe()
  private val FAKE_CLIENT_2 = Client.asActor(UUID.randomUUID.toString, client2TestProbe.ref)


  var room: ServerRoom = _
  var roomHandler: RoomHandler = _
  var roomActor: ActorRef = _


  before {
    room = ServerRoom()
    roomHandler = RoomHandler()
    roomActor = system actorOf RoomActor(room, roomHandler)
  }

  after {
    roomActor ! PoisonPill
  }

  override def beforeAll(): Unit = {}

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A room actor" should {
    "allow clients to join" in {
      roomActor ! Join(FAKE_CLIENT_1, Room.defaultPublicPassword)
      expectMsg(JoinOk)
      assert(room.connectedClients.contains(FAKE_CLIENT_1))
    }

    "allow client to leave the room" in {
      roomActor ! Join(FAKE_CLIENT_1, Room.defaultPublicPassword)
      expectMsg(JoinOk)
      roomActor ! Leave(FAKE_CLIENT_1)
      expectMsg(ClientLeaved)
      assert(!room.connectedClients.contains(FAKE_CLIENT_1))
    }

    "respond with ClientNotAuthorized when receives a message from a client that hasn't join the room" in {
      roomActor ! Msg(FAKE_CLIENT_2, "test-message")
      expectMsg(ClientNotAuthorized)
    }

    "stop himself when the room is closed" in {
      val probe = TestProbe()
      probe watch roomActor
      room.close()
      probe.expectTerminated(roomActor)
    }
  }


}
