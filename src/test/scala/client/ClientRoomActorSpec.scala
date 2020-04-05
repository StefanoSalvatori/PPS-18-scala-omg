package client

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestKit}
import client.room.{ClientRoom, ClientRoomActor}
import client.utils.MessageDictionary.CreatePublicRoom
import com.typesafe.config.ConfigFactory
import common.{Routes, TestConfig}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import server.GameServer
import server.room.ServerRoom

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.util.Success

class ClientRoomActorSpec extends TestKit(ActorSystem("ClientSystem", ConfigFactory.load()))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll
  with TestConfig {

  private val serverAddress = "localhost"
  private val serverPort = CLIENT_ROOM_ACTOR_SPEC_SERVER_PORT
  private val serverUri = Routes.httpUri(serverAddress, serverPort)

  private val ROOM_TYPE_NAME: String = "test_room"

  implicit val executionContext: ExecutionContext = system.dispatcher
  private val requestTimeout = 5 // Seconds

  import akka.util.Timeout

  implicit val timeout: Timeout = Timeout(requestTimeout, TimeUnit.SECONDS)

  private var coreClient: ActorRef = _
  private var clientRoomActor: ActorRef = _
  private var gameServer: GameServer = _

  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

  before {
    coreClient = system actorOf CoreClient(serverUri)
    gameServer = GameServer(serverAddress, serverPort)
    gameServer.defineRoom(ROOM_TYPE_NAME, ServerRoom(_))
    Await.ready(gameServer.start(), 5 seconds)

    coreClient ! CreatePublicRoom(ROOM_TYPE_NAME, Set.empty)
    val room = expectMsgType[Success[ClientRoom]]
    clientRoomActor = system actorOf ClientRoomActor(coreClient, serverUri, room.value)

  }

}
