package client

import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.scalalogging.LazyLogging
import common.{RoomJsonSupport, TestConfig}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import server.GameServer
import server.room.ServerRoom

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class ClientSpec extends AnyFlatSpec
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll
  with TestConfig
  with ScalatestRouteTest
  with LazyLogging
with RoomJsonSupport{

  private val serverAddress = "localhost"
  private val serverPort = CLIENT_SPEC_SERVER_PORT

  private val ROOM_TYPE_NAME: String = "test_room"
  private val SERVER_LAUNCH_AWAIT_TIME = 10 seconds
  private val SERVER_SHUTDOWN_AWAIT_TIME = 10 seconds

  implicit val execContext: ExecutionContextExecutor = system.dispatcher
  private var gameServer: GameServer = _
  private var client: Client = _


  behavior of "Client"

  before {
    gameServer = GameServer(serverAddress, serverPort)
    gameServer.defineRoom(ROOM_TYPE_NAME, ServerRoom(_))
    Await.ready(gameServer.start(), SERVER_LAUNCH_AWAIT_TIME)
    logger debug s"Server started at $serverAddress:$serverPort"

    client = Client(serverAddress, serverPort)
  }

  after {
    Await.ready(gameServer.shutdown(), SERVER_SHUTDOWN_AWAIT_TIME)
  }

  it should "start with no joined rooms" in {
    client.joinedRooms shouldBe empty
  }

  it should "create public rooms" in {
    val r = Await.result(client createPublicRoom(ROOM_TYPE_NAME, ""), 5 seconds)
    val roomList = Await.result(client getAvailableRoomsByType ROOM_TYPE_NAME, 5 seconds)
    roomList should have size 1
  }

  it should "fail on create public rooms if server is unreachable" in {
    Await.ready(gameServer.shutdown(), SERVER_SHUTDOWN_AWAIT_TIME)

    assertThrows[Exception] {
      Await.result(client createPublicRoom(ROOM_TYPE_NAME, ""), 5 seconds)
    }
  }

  it should "fail on getting rooms  if server is unreachable" in {
    Await.ready(gameServer.shutdown(), SERVER_SHUTDOWN_AWAIT_TIME)

    assertThrows[Exception] {
      Await.result(client getAvailableRoomsByType (ROOM_TYPE_NAME), 5 seconds)
    }
  }

  it should "get all available rooms of specific type" in {
    Await.result(client createPublicRoom(ROOM_TYPE_NAME, ""), 5 seconds)
    Await.result(client createPublicRoom(ROOM_TYPE_NAME, ""), 5 seconds)
    val roomList = Await.result(client getAvailableRoomsByType (ROOM_TYPE_NAME), 5 seconds)
    roomList should have size 2
  }

  //TODO: check this test
  it should "create a public room and automatically join such room" in {
    client createPublicRoom(ROOM_TYPE_NAME, "") onComplete {
      case Success(_) => client.joinedRooms should have size 1
      case Failure(exception) => logger error exception.toString
    }
  }


}
