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
import common.communication.CommunicationProtocol.RoomProtocolMessage
import common.room.Room
import server.RoomHandler
class RoomActorSpec extends TestKit(ActorSystem("Rooms", ConfigFactory.load()))
  with ImplicitSender
  with Matchers
  with AnyWordSpecLike
  with BeforeAndAfter
  with BeforeAndAfterAll {


  private val FakeClient_1 = makeClient()
  private val FakeClient_2 = makeClient()


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
      roomActor ! Join(FakeClient_1, "", Room.defaultPublicPassword)
      val res = expectMsgType[RoomProtocolMessage]
      res.messageType shouldBe JoinOk
      assert(room.connectedClients.contains(FakeClient_1))
    }

    "allow client to leave the room" in {
      roomActor ! Join(FakeClient_1, "", Room.defaultPublicPassword)
      val res = expectMsgType[RoomProtocolMessage]
      res.messageType shouldBe JoinOk
      roomActor ! Leave(FakeClient_1)
      expectMsg(ClientLeaved)
      assert(!room.connectedClients.contains(FakeClient_1))
    }

    "allow client to reconnect to a room and respond JoinOk" in {
      val testClient = makeClient()
      roomActor ! Join(testClient, "", Room.defaultPublicPassword)
      val res = expectMsgType[RoomProtocolMessage]
      roomActor ! Leave(testClient)
      expectMsg(ClientLeaved)

      room.allowReconnection(testClient, 5000)
      val fakeClient = makeClient(res.sessionId)

      roomActor ! Join(fakeClient, fakeClient.id, Room.defaultPublicPassword)
      val reconnectResponse = expectMsgType[RoomProtocolMessage]
      reconnectResponse.messageType shouldBe JoinOk
      assert(room.connectedClients.contains(testClient))

    }


    "respond ClientNotAuthorized on fail reconnection" in {
      roomActor ! Join(FakeClient_1, "", Room.defaultPublicPassword)
      val res = expectMsgType[RoomProtocolMessage]
      roomActor ! Leave(FakeClient_1)
      expectMsg(ClientLeaved)

      //do not allow reconnection

      val fakeClient = makeClient(res.sessionId)

      roomActor ! Join(fakeClient, fakeClient.id, Room.defaultPublicPassword)
      val reconnectResponse = expectMsgType[RoomProtocolMessage]
      reconnectResponse.messageType shouldBe ClientNotAuthorized


    }

    "respond with ClientNotAuthorized when receives a message from a client that hasn't join the room" in {
      roomActor ! Msg(FakeClient_2, "test-message")
      val res = expectMsgType[RoomProtocolMessage]
      res.messageType shouldBe ClientNotAuthorized
    }

    "stop himself when the room is closed" in {
      val probe = TestProbe()
      probe watch roomActor
      room.close()
      probe.expectTerminated(roomActor)
    }
  }


  private def makeClient(id:String = UUID.randomUUID.toString): Client = {
    val client1TestProbe = TestProbe()
    Client.asActor(id, client1TestProbe.ref)
  }


}
