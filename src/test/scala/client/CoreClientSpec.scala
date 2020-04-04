package client

import java.util.concurrent.TimeUnit

import akka.pattern.ask
import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatest.wordspec.AnyWordSpecLike
import client.room.ClientRoom
import client.utils.MessageDictionary._
import common.{FilterOptions, Routes, TestConfig}
import org.scalatest.matchers.should.Matchers
import server.GameServer
import server.room.ServerRoom

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class CoreClientSpec extends TestKit(ActorSystem("ClientSystem", ConfigFactory.load()))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll
  with TestConfig {

  private val serverAddress = "localhost"
  private val serverPort = CORE_CLIENT_SPEC_SERVER_PORT
  private val serverUri = Routes.uri(serverAddress, serverPort)

  private val ROOM_TYPE_NAME: String = "test_room"

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
    gameServer.defineRoom(ROOM_TYPE_NAME, ServerRoom(_))
    Await.ready(gameServer.start(), 5 seconds)

    coreClient ! CreatePublicRoom(ROOM_TYPE_NAME, Set.empty)
    coreClient ! CreatePublicRoom(ROOM_TYPE_NAME, Set.empty)
  }

  "Regarding joined rooms, a core client" must {
    "start with no joined rooms" in {
      (coreClient ? GetJoinedRooms).onComplete(reply => {
        expectMsgClass(classOf[JoinedRooms])
        assert(reply.asInstanceOf[JoinedRooms].joinedRooms.isEmpty)
      })
    }

    "add a room to joined room if such room was not previously joined" in {
      coreClient ! Join(ROOM_TYPE_NAME, FilterOptions.empty())
      (coreClient ? GetJoinedRooms).onComplete(reply => {
        expectMsgClass(classOf[JoinedRooms])
        reply.asInstanceOf[JoinedRooms].joinedRooms should have size 1
      })

      coreClient ! Join(ROOM_TYPE_NAME, FilterOptions.empty())
      (coreClient ? GetJoinedRooms).onComplete(reply => {
        expectMsgClass(classOf[JoinedRooms])
        reply.asInstanceOf[JoinedRooms].joinedRooms should have size 2
      })
    }

    "automatically join a created room" in {
      coreClient ! CreatePublicRoom(ROOM_TYPE_NAME, Set.empty)
      (coreClient ? GetJoinedRooms).onComplete(reply => {
        reply.asInstanceOf[JoinedRooms].joinedRooms should have size 1
      })
    }

    "not add an already joined room" in {
      val createRoomFuture = (coreClient ? CreatePublicRoom(ROOM_TYPE_NAME, Set.empty)) flatMap {
        case Success(room) => Future.successful(room.asInstanceOf[ClientRoom])
        case Failure(ex) => Future.failed(ex)
      }
      val room = Await.result(createRoomFuture, 5 seconds)

      Await.result((coreClient ? JoinById(room.roomId)), 5 seconds) match {
        case Failure(_) => succeed
        case _ => fail
      }
    }
  }

  "When the client creates a new public room, the core client" must {
    "add the new room to the set of joined rooms" in {
      coreClient ! CreatePublicRoom(ROOM_TYPE_NAME, Set.empty)
      Thread sleep 1000
      (coreClient ? GetJoinedRooms).onComplete(reply => {
        expectMsgClass(classOf[JoinedRooms])
        reply.asInstanceOf[JoinedRooms].joinedRooms should have size 1
      })
    }
  }
}


