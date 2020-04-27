package client

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import client.room.{ClientRoom, ClientRoomActor, JoinedRoom, JoinedRoomImpl}
import client.utils.MessageDictionary._
import com.typesafe.config.ConfigFactory
import common.http.Routes
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import server.GameServer
import server.room.ServerRoom
import test_utils.TestConfig

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success, Try}

class CoreClientSpec extends TestKit(ActorSystem("ClientSystem", ConfigFactory.load()))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll
  with TestConfig {

  private val serverAddress = "localhost"
  private val serverPort = CoreClientSpecServerPort
  private val serverUri = Routes.httpUri(serverAddress, serverPort)

  private val RoomTypeName: String = "test_room"

  implicit val executionContext: ExecutionContext = system.dispatcher
  private val requestTimeout = 5 // Seconds

  import akka.util.Timeout

  implicit val timeout: Timeout = Timeout(requestTimeout, TimeUnit.SECONDS)

  private var coreClient: ActorRef = _
  private var gameServer: GameServer = _

  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

  before {
    coreClient = system actorOf CoreClient(serverUri)
    gameServer = GameServer(serverAddress, serverPort)
    gameServer.defineRoom(RoomTypeName, () => ServerRoom())
    Await.ready(gameServer.start(), 5 seconds)

    Await.ready(coreClient ? CreatePublicRoom(RoomTypeName, Set.empty), 5 seconds)
    Await.ready(coreClient ? CreatePublicRoom(RoomTypeName, Set.empty), 5 seconds)
  }

  "Regarding joined rooms, a core client" must {
    "start with no joined rooms" in {
      coreClient ! GetJoinedRooms
      val res = expectMsgType[JoinedRooms]
      res.joinedRooms shouldBe empty

    }

    "keep track of joined rooms" in {
      val rooms = (0 to 3).map(i => ClientRoom.createJoinable(coreClient, "", i.toString, Set()))
      val refs = rooms.map(r => system.actorOf(MockClientRoomActor(r)))

      refs foreach { t =>
        coreClient.tell(ClientRoomActorJoined, sender = t)
      }
      coreClient.tell(ClientRoomActorLeft, sender = refs.head)

      coreClient ! GetJoinedRooms
      val res = expectMsgType[JoinedRooms]
      res.joinedRooms should have size 3

    }

    "respond with the created room if request to create a room" in {
      coreClient ! CreatePublicRoom(RoomTypeName, Set.empty)
      val tryRes = expectMsgType[Try[ClientRoom]]
      if (tryRes.isSuccess) assert(tryRes.get.isInstanceOf[ClientRoom])

    }

  }

}

object MockClientRoomActor {
  def apply(room: ClientRoom): Props = Props(classOf[MockClientRoomActor],  room)
}
class MockClientRoomActor(room: ClientRoom) extends Actor {
  private implicit val system: ActorSystem = context.system

  override def receive: Receive = {
    case RetrieveClientRoom =>
      sender ! ClientRoomResponse(JoinedRoom(self, "", room.roomId, Set()))
  }
}


