package client

import java.util.concurrent.TimeUnit

import akka.pattern.ask
import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatest.wordspec.AnyWordSpecLike
import client.MessageDictionary._
import client.room.ClientRoom
import common.CommonRoom.Room
import common.Routes
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

class CoreClientSpec extends TestKit(ActorSystem("ClientSystem", ConfigFactory.load()))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll {

  private val serverAddress = "localhost"
  private val serverPort = 8080
  private val serverUri = Routes.uri(serverAddress, serverPort)

  private val ROOM_TYPE_NAME: String = "test_room"

  implicit val executionContext: ExecutionContext = system.dispatcher
  private val requestTimeout = 5 // Seconds
  import akka.util.Timeout
  implicit val timeout: Timeout = Timeout(requestTimeout, TimeUnit.SECONDS)

  private var coreClient: ActorRef = _

  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

  before {
    coreClient = system actorOf CoreClient(serverUri)
  }

  "Regarding joined rooms, a core client" must {
    val A = "A"; val B = "B"
    val roomA = ClientRoom(serverUri, "A"); val roomB = ClientRoom(serverUri, "B")

    "start with no joined rooms" in {
      (coreClient ? GetJoinedRooms).onComplete(reply => {
        expectMsgClass(classOf[JoinedRooms])
        assert(reply.asInstanceOf[JoinedRooms].rooms.isEmpty)
      })
    }

    "add a room to joined room if such room was not previously joined" in {
      coreClient ! NewJoinedRoom(roomA)
      (coreClient ? GetJoinedRooms).onComplete(reply => {
        expectMsgClass(classOf[JoinedRooms])
        reply.asInstanceOf[JoinedRooms].rooms shouldEqual Set(roomA)
      })

      coreClient ! NewJoinedRoom(roomB)
      (coreClient ? GetJoinedRooms).onComplete(reply => {
        expectMsgClass(classOf[JoinedRooms])
        reply.asInstanceOf[JoinedRooms].rooms shouldEqual Set(roomA, roomB)
      })
    }

    "not add an already joined room" in {
      coreClient ! NewJoinedRoom(roomA)
      (coreClient ? GetJoinedRooms).onComplete(reply => {
        expectMsgClass(classOf[JoinedRooms])
        reply.asInstanceOf[JoinedRooms].rooms shouldEqual Set(roomA, roomB)
      })
    }
  }

  "When the client creates a new public room, the core client" must {
    "add the new room to the set of joined rooms" in {
      coreClient ! CreatePublicRoom(ROOM_TYPE_NAME, "")
      Thread sleep 1000
      (coreClient ? GetJoinedRooms).onComplete(reply => {
        expectMsgClass(classOf[JoinedRooms])
        reply.asInstanceOf[JoinedRooms].rooms.size shouldEqual 1
      })
    }
  }
}


