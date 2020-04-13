package client

import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestKit
import com.typesafe.scalalogging.LazyLogging
import common.room.{FilterOptions, RoomJsonSupport, RoomProperty}
import common.TestConfig
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import server.GameServer
import server.room.ServerRoom
import server.utils.ExampleRooms
import server.utils.ExampleRooms.{ClosableRoomWithState, MyRoom, NoPropertyRoom}
import common.room.RoomPropertyValueConversions._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, TimeoutException}

class ClientSpec extends AnyFlatSpec
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll
  with TestConfig
  with ScalatestRouteTest
  with LazyLogging
  with RoomJsonSupport {

  private val DefaultTimeout = 5 seconds

  private val serverAddress = "localhost"
  private val serverPort = CLIENT_SPEC_SERVER_PORT

  private val ROOM_TYPE_NAME: String = "test_room"
  private val SERVER_LAUNCH_AWAIT_TIME = 10 seconds
  private val SERVER_SHUTDOWN_AWAIT_TIME = 10 seconds

  implicit val execContext: ExecutionContextExecutor = system.dispatcher
  private var gameServer: GameServer = _
  private var client: Client = _
  private var client2: Client = _

  behavior of "Client"

  val testProperties = Set(RoomProperty("a", 1), RoomProperty("b", "qwe"))

  before {
    gameServer = GameServer(serverAddress, serverPort)
    gameServer.defineRoom(ROOM_TYPE_NAME, () => ServerRoom())
    gameServer.defineRoom(ExampleRooms.myRoomType, MyRoom)
    gameServer.defineRoom(ExampleRooms.noPropertyRoomType, NoPropertyRoom)
    gameServer.defineRoom(ExampleRooms.closableRoomWithStateType, ClosableRoomWithState)

    Await.ready(gameServer.start(), SERVER_LAUNCH_AWAIT_TIME)
    logger debug s"Server started at $serverAddress:$serverPort"

    client = Client(serverAddress, serverPort)
    client2 = Client(serverAddress, serverPort)
  }

  after {
    // Await.ready(gameServer.stop(), SERVER_SHUTDOWN_AWAIT_TIME)
    Await.ready(gameServer.terminate(), SERVER_SHUTDOWN_AWAIT_TIME)
  }

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  it should "start with no joined rooms" in {
    client.joinedRooms shouldBe empty
  }

  it should "create public rooms" in {
    val roomList1 = Await.result(client getAvailableRoomsByType(ROOM_TYPE_NAME, FilterOptions.empty), DefaultTimeout)
    roomList1 should have size 0
    val r = Await.result(client createPublicRoom ROOM_TYPE_NAME, DefaultTimeout)
    val roomList2 = Await.result(client getAvailableRoomsByType(ROOM_TYPE_NAME, FilterOptions.empty), DefaultTimeout)
    roomList2 should have size 1
  }

  it should "create public room and get such rooms asking the available rooms" in {
    val r = Await.result(client createPublicRoom(ROOM_TYPE_NAME, Set.empty), DefaultTimeout)
    val roomList = Await.result(client getAvailableRoomsByType(ROOM_TYPE_NAME, FilterOptions.empty), DefaultTimeout)
    roomList should have size 1
  }

  it should "fail on create public rooms if server is unreachable" in {
    Await.ready(gameServer.stop(), SERVER_SHUTDOWN_AWAIT_TIME)

    assertThrows[Exception] {
      Await.result(client createPublicRoom ROOM_TYPE_NAME, DefaultTimeout)
    }
  }

  it should "fail on getting rooms  if server is stopped" in {
    Await.ready(gameServer.stop(), SERVER_SHUTDOWN_AWAIT_TIME)
    assertThrows[Exception] {
      Await.result(client getAvailableRoomsByType(ROOM_TYPE_NAME, FilterOptions.empty), DefaultTimeout)
    }
  }

  it should "get all available rooms of specific type" in {
    Await.result(client createPublicRoom ROOM_TYPE_NAME, DefaultTimeout)
    Await.result(client createPublicRoom ROOM_TYPE_NAME, DefaultTimeout)
    val roomList = Await.result(client getAvailableRoomsByType(ROOM_TYPE_NAME, FilterOptions.empty), DefaultTimeout)
    roomList should have size 2
  }

  it should "fail on joining a room if server is unreachable" in {
    Await.result(client createPublicRoom ROOM_TYPE_NAME, DefaultTimeout)
    Await.ready(gameServer.stop(), SERVER_SHUTDOWN_AWAIT_TIME)

    assertThrows[Exception] {
      Await.result(client join(ROOM_TYPE_NAME, FilterOptions.empty), DefaultTimeout)
    }
  }

  it should "fail on joining a room of a type that does not exists" in {
    assertThrows[Exception] {
      Await.result(client join(ROOM_TYPE_NAME, FilterOptions.empty), DefaultTimeout)
    }
  }

  it should "be able to join a room by it's id" in {
    val room1 = Await.result(client.createPublicRoom(ROOM_TYPE_NAME, Set.empty), DefaultTimeout)
    val room2 = Await.result(client2.joinById(room1.roomId), DefaultTimeout)
    val joinedRooms = client2 joinedRooms()
    joinedRooms should have size 1
    room2.roomId shouldEqual room1.roomId
  }

  it should "join a room or create it if it does not exists" in {
    val room = Await.result(client.joinOrCreate(ROOM_TYPE_NAME, FilterOptions.empty),  DefaultTimeout)
    client joinedRooms() should have size 1
    val roomOnServer = Await.result(client.getAvailableRoomsByType(ROOM_TYPE_NAME, FilterOptions.empty), DefaultTimeout)
    assert(roomOnServer.map(_.roomId).contains(room.roomId))
  }

  it should "be able to join an existing room" in {
    val room = Await.result(client createPublicRoom ROOM_TYPE_NAME, DefaultTimeout)
    val joined = Await.result(client join(ROOM_TYPE_NAME, FilterOptions.empty), DefaultTimeout)
    assert(room.roomId == joined.roomId)
  }

  it should "create a public room and join such room" in {
    val room = Await.result(client createPublicRoom ROOM_TYPE_NAME, DefaultTimeout)
    assert(client.joinedRooms().exists(_.roomId==room.roomId))
  }

  it should "fail on joining an already joined room" in {
    val room = Await.result(client createPublicRoom ROOM_TYPE_NAME , DefaultTimeout)
    assertThrows[Exception] {
      Await.result(client joinById room.roomId, DefaultTimeout)
    }
  }

  it should "not create a room if an available room exists " in {
    Await.result(client.createPublicRoom(ROOM_TYPE_NAME), DefaultTimeout)
    Await.result(client2.joinOrCreate(ROOM_TYPE_NAME, FilterOptions.empty), DefaultTimeout)
    val roomsOnServer = Await.result(client.getAvailableRoomsByType(ROOM_TYPE_NAME, FilterOptions.empty), DefaultTimeout)
    roomsOnServer should have size 1
  }

  it should "join a private room if the right password is provided" in {
    val room = Await.result(client.createPrivateRoom(ROOM_TYPE_NAME, password = "pwd"), DefaultTimeout)
    val res = Await.result(client2.joinById(room.roomId, "pwd"), DefaultTimeout)
    assert(res == room)
  }

  it should "not join a private room if a wrong password is provided" in {
    val room = Await.result(client.createPrivateRoom(ROOM_TYPE_NAME, password = "pwd"), DefaultTimeout)
    assertThrows[TimeoutException] {
      Await.result(client2.joinById(room.roomId, "pwd2"), DefaultTimeout)
    }
  }
}
