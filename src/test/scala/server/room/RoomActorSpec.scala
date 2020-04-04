package server.room

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import server.room.RoomActor._

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


  var room: ServerRoom = ServerRoom(UUID.randomUUID().toString)
  var roomActor: ActorRef = system actorOf RoomActor(room)


  before {
    room = ServerRoom(UUID.randomUUID().toString)
    roomActor = system actorOf RoomActor(room)
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
      roomActor ! Join(FAKE_CLIENT_1)
      expectMsg(JoinOk)
      assert(room.connectedClients.contains(FAKE_CLIENT_1))
    }

    "allow client to leave the room" in {
      roomActor ! Join(FAKE_CLIENT_1)
      expectMsg(JoinOk)
      roomActor ! Leave(FAKE_CLIENT_1)
      expectMsg(ClientLeaved)
      assert(!room.connectedClients.contains(FAKE_CLIENT_1))
    }

    "respond with ClientNotAuthorized when receives a message from a client that hasn't join the room" in {
      roomActor ! Msg(FAKE_CLIENT_2, "test-message")
      expectMsg(ClientNotAuthorized)
    }

    /*"send a message to a specific client when receives Tell" in {
      roomActorRef ! Join(FAKE_CLIENT_1, testActor)
      expectMsg(JoinOk)
      val message = "Hello"
      roomActorRef ! Tell(FAKE_CLIENT_1, message)
      expectMsg(message)
    }*/

    /*"broadcast a message to all client when receives NotifyAll" in {
      val client2 = TestProbe()
      roomActorRef ! Join(FAKE_CLIENT_2, client2.ref)
      client2.expectMsg(JoinOk)
      val message = "Broadcast"
      roomActorRef ! NotifyAll(message)
      expectMsg(message)
      client2.expectMsg(message)

    }*/
  }


}
