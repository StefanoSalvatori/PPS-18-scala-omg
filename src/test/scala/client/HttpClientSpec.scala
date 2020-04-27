package client

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import client.utils.MessageDictionary._
import com.typesafe.config.ConfigFactory
import common.communication.BinaryProtocolSerializer
import common.http.Routes
import common.room.{FilterOptions, SharedRoom}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import server.GameServer
import server.room.ServerRoom
import test_utils.TestConfig

import scala.concurrent.Await

class HttpClientSpec extends TestKit(ActorSystem("ClientSystem", ConfigFactory.load()))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll
  with TestConfig {

  private val serverAddress = "localhost"
  private val serverPort = HttpClientSpecServerPort
  private val httpServerUri = Routes.httpUri(serverAddress, serverPort)

  private val RoomTypeName: String = "test_room"

  private var gameServer: GameServer = _

  override def beforeAll: Unit = {
    gameServer = GameServer(serverAddress, serverPort)
    gameServer.defineRoom(RoomTypeName, () => ServerRoom())
    Await.ready(gameServer.start(), ServerLaunchAwaitTime)
  }

  override def afterAll: Unit = {
    Await.ready(gameServer.terminate(), ServerShutdownAwaitTime)
    TestKit.shutdownActorSystem(system)
  }

  "An Http client actor" must {

    val httpTestActor: ActorRef = system actorOf HttpClient(httpServerUri)


    "when asked to post a room, return the new room" in {
      httpTestActor ! HttpPostRoom(RoomTypeName, Set.empty)

      expectMsgPF() {
        case HttpRoomResponse(room) =>
          assert(room.isInstanceOf[SharedRoom])
        case FailResponse(_) =>
      }

    }

    "when asked to get a rooms, return a set of rooms" in {
      httpTestActor ! HttpGetRooms(RoomTypeName, FilterOptions.empty)

      expectMsgPF() {
        case HttpRoomSequenceResponse(rooms) =>  assert(rooms.isInstanceOf[Seq[SharedRoom]])
        case FailResponse(_) =>
      }
    }

    "when asked to open a web socket, return an actor ref related to that socket" in {
      httpTestActor ! HttpPostRoom(RoomTypeName, Set.empty)
      val roomRes = expectMsgType[HttpRoomResponse]

      httpTestActor ! HttpRoomSocketRequest(roomRes.room.roomId, BinaryProtocolSerializer())

      expectMsgPF() {
        case HttpSocketSuccess(ref) =>  assert(ref.isInstanceOf[ActorRef])
        case HttpSocketFail(_) =>
      }
    }
  }
}
