package client

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import client.MessageDictionary._
import com.typesafe.config.ConfigFactory
import common.{Routes, TestConfig}
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
  private val serverUri = Routes.uri(serverAddress, serverPort)

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

    val probe = TestProbe()
    val httpTestActor: ActorRef = system actorOf HttpClient(serverUri, probe.ref)

    //TODO: check this test
    /*"when asked to create a new public room, return the new room" in {
      httpTestActor ! CreatePublicRoom(ROOM_TYPE_NAME)
      probe expectMsgClass classOf[NewJoinedRoom]
    }*/
  }
}
