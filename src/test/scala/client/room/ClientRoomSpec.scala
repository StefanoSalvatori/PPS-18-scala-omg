package client.room

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestKit
import client.utils.MessageDictionary.{CreatePublicRoom, GetJoinedRooms, JoinedRooms}
import client.{Client, CoreClient}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import common.TestConfig
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import server.GameServer
import server.room.ServerRoom
import akka.pattern.ask
import akka.util.Timeout
import common.http.Routes
import common.room.RoomJsonSupport

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.util.Try

class ClientRoomSpec extends TestKit(ActorSystem("ClientSystem", ConfigFactory.load()))
  with TestConfig
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll
  with LazyLogging
  with RoomJsonSupport {

  private val ServerAddress = "localhost"
  private val ServerPort = CLIENT_ROOM_SPEC_SERVER_PORT

  private val RoomTypeName: String = "test_room"
  private val ServerLaunchAwaitTime = 10 seconds
  private val ServerShutdownAwaitTime = 10 seconds
  implicit private val DefaultTimeout: Timeout = 5 seconds
  implicit private val DefaultDuration: Duration = 5 seconds

  implicit val execContext: ExecutionContextExecutor = system.dispatcher
  private var gameServer: GameServer = _
  private var coreClient: ActorRef = _
  private var clientRoom: ClientRoom = _

  before {
    gameServer = GameServer(ServerAddress, ServerPort)
    gameServer.defineRoom(RoomTypeName, () => ServerRoom())
    Await.ready(gameServer.start(), ServerLaunchAwaitTime)
    logger debug s"Server started at $ServerAddress:$ServerPort"
    coreClient = system actorOf (CoreClient(Routes.httpUri(ServerAddress, ServerPort)))
    val res = Await.result((coreClient ? CreatePublicRoom(RoomTypeName, Set.empty)).mapTo[Try[ClientRoom]], DefaultDuration)
    clientRoom = res.get
  }

  after {
    Await.ready(gameServer.terminate(), ServerShutdownAwaitTime)
  }

  "A client room" must {
    "join and notify the core client" in {
      Await.result(clientRoom.join(), DefaultDuration)
      val res = Await.result( (coreClient ? GetJoinedRooms).mapTo[JoinedRooms], DefaultDuration).joinedRooms
      res should have size 1
    }

    "leave and notify the core client" in {
      Await.result(clientRoom.join(), DefaultDuration)
      Await.result(clientRoom.leave(), DefaultDuration)

      val res = Await.result( (coreClient ? GetJoinedRooms).mapTo[JoinedRooms], DefaultDuration).joinedRooms
      res should have size 0
    }

    "execute a defined callback on message recived from server room" in {
    }

    "send messages to server room" in {
    }

  }
}
