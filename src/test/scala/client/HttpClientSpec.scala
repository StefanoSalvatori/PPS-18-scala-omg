package client

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import client.utils.MessageDictionary._
import com.typesafe.config.ConfigFactory
import common.SharedRoom.Room
import common.{FilterOptions, Routes, TestConfig}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import server.GameServer
import server.room.ServerRoom

import scala.concurrent.Await
import scala.concurrent.duration._

class HttpClientSpec extends TestKit(ActorSystem("ClientSystem", ConfigFactory.load()))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll
  with TestConfig {

  private val serverAddress = "localhost"
  private val serverPort = HTTP_CLIENT_SPEC_SERVER_PORT
  private val httpServerUri = Routes.httpUri(serverAddress, serverPort)

  private val ROOM_TYPE_NAME: String = "test_room"
  private val SERVER_LAUNCH_AWAIT_TIME = 10 seconds
  private val SERVER_SHUTDOWN_AWAIT_TIME = 10 seconds


  private var gameServer: GameServer = _

  override def beforeAll: Unit = {
    gameServer = GameServer(serverAddress, serverPort)
    gameServer.defineRoom(ROOM_TYPE_NAME, id => ServerRoom(id))
    Await.ready(gameServer.start(), SERVER_LAUNCH_AWAIT_TIME)
  }

  override def afterAll: Unit = {
    Await.ready(gameServer.shutdown(), SERVER_SHUTDOWN_AWAIT_TIME)
    TestKit.shutdownActorSystem(system)
  }

  "An Http client actor" must {

    val httpTestActor: ActorRef = system actorOf HttpClient(httpServerUri)


    "when asked to post a room, return the new room" in {
      httpTestActor ! HttpPostRoom(ROOM_TYPE_NAME, Set.empty)
      val r = expectMsgType[RoomResponse]
      assert(r.room.isInstanceOf[Room])
    }

    "when asked to get a rooms, return a set of rooms" in {
      httpTestActor ! HttpGetRooms(ROOM_TYPE_NAME, FilterOptions.empty())
      val r = expectMsgType[RoomSequenceResponse]
      assert(r.rooms.isInstanceOf[Seq[Room]])
    }
  }
}
