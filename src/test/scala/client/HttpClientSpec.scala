package client

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import client.MessageDictionary._
import common.Routes
import server.GameServer
import server.examples.DefineRoomType.{HOST, PORT, ROOM_TYPE_NAME, gameServer}
import server.room.RoomStrategy

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.StdIn

class HttpClientSpec extends TestKit(ActorSystem("ClientSystem", ConfigFactory.load()))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {

  private val serverAddress = "localhost"
  private val serverPort = 8080
  private val serverUri = Routes.uri(serverAddress, serverPort)

  private val ROOM_TYPE_NAME: String = "test_room"
  private val SERVER_LAUNCH_AWAIT_TIME = 10 // sec

  private var gameServer: GameServer = _

  override def beforeAll: Unit = {
    gameServer = GameServer(serverAddress, serverPort)
    gameServer onStart {
      println("GAMESERVER STARTED")
    }
    gameServer onShutdown {
      println("GAMESERVER IS DOWN :-(")
    }

    gameServer.defineRoom(ROOM_TYPE_NAME, new RoomStrategy {
      override def onJoin(): Unit = {}
      override def onMessageReceived(): Unit = {}
      override def onLeave(): Unit = {}
      override def onCreate(): Unit = {}
    })

    Await.ready(gameServer.start(), Duration(SERVER_LAUNCH_AWAIT_TIME, TimeUnit.SECONDS))
    println(s"try http://$serverAddress:$serverPort/rooms/$ROOM_TYPE_NAME from your browser")
  }

  override def afterAll: Unit = {
    gameServer.shutdown()
    TestKit.shutdownActorSystem(system)
  }

  "An Http client actor" must {

    val probe = TestProbe()
    val httpTestActor: ActorRef = system actorOf HttpClient(serverUri, probe.ref)

    "when asked to create a new public room, return the new room" in {
      httpTestActor ! CreatePublicRoom
      probe expectMsgClass classOf[NewJoinedRoom]
    }
  }
}
