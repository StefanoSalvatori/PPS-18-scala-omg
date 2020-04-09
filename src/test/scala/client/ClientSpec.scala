package client

import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestKit
import com.typesafe.scalalogging.LazyLogging
import common.room.{FilterOptions, IntRoomPropertyValue, RoomJsonSupport, RoomProperty, StringRoomPropertyValue}
import common.TestConfig
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import server.GameServer
import server.room. ServerRoom
import server.utils.ExampleRooms
import server.utils.ExampleRooms.{MyRoom, NoPropertyRoom}
import common.room.BasicRoomPropertyValueConversions._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}

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

  behavior of "Client"

  before {
    gameServer = GameServer(serverAddress, serverPort)
    gameServer.defineRoom(ROOM_TYPE_NAME, ServerRoom(_))
    gameServer.defineRoom(ExampleRooms.myRoomType, new MyRoom(_))
    gameServer.defineRoom(ExampleRooms.noPropertyRoomType, new NoPropertyRoom(_))
    Await.ready(gameServer.start(), SERVER_LAUNCH_AWAIT_TIME)
    logger debug s"Server started at $serverAddress:$serverPort"

    client = Client(serverAddress, serverPort)
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
    val r = Await.result(client createPublicRoom(ROOM_TYPE_NAME, Set.empty), DefaultTimeout)
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
      Await.result(client createPublicRoom(ROOM_TYPE_NAME, Set.empty), DefaultTimeout)
    }
  }

  it should "fail on getting rooms  if server is stopped" in {
    Await.ready(gameServer.stop(), SERVER_SHUTDOWN_AWAIT_TIME)
    assertThrows[Exception] {
      Await.result(client getAvailableRoomsByType(ROOM_TYPE_NAME, FilterOptions.empty), DefaultTimeout)
    }
  }

  it should "get all available rooms of specific type" in {
    Await.result(client createPublicRoom(ROOM_TYPE_NAME, Set.empty), DefaultTimeout)
    Await.result(client createPublicRoom(ROOM_TYPE_NAME, Set.empty), DefaultTimeout)
    val roomList = Await.result(client getAvailableRoomsByType(ROOM_TYPE_NAME, FilterOptions.empty), DefaultTimeout)
    roomList should have size 2
  }

  it should "fail on joining a room if server is unreachable" in {
    Await.result(client createPublicRoom(ROOM_TYPE_NAME, Set.empty), DefaultTimeout)
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
    val client1 = Client(serverAddress, serverPort)
    val client2 = Client(serverAddress, serverPort)
    val room1 = Await.result(client1.createPublicRoom(ROOM_TYPE_NAME, Set.empty), DefaultTimeout)
    val room2 = Await.result(client2.joinById(room1.roomId), DefaultTimeout)
    val joinedRooms = client2 joinedRooms()
    joinedRooms should have size 1
    room2.roomId shouldEqual room1.roomId
  }

  it should "join a room or create it if it does not exists" in {
    val room = Await.result(client.joinOrCreate(ROOM_TYPE_NAME, FilterOptions.empty, Set.empty),  DefaultTimeout)
    client joinedRooms() should have size 1
    val roomOnServer = Await.result(client.getAvailableRoomsByType(ROOM_TYPE_NAME, FilterOptions.empty), DefaultTimeout)
    assert(roomOnServer.map(_.roomId).contains(room.roomId))
  }

  it should "be able to join an existing room" in {
    val room = Await.result(client createPublicRoom(ROOM_TYPE_NAME, Set.empty), DefaultTimeout)
    val joined = Await.result(client join(ROOM_TYPE_NAME, FilterOptions.empty), DefaultTimeout)
    assert(room.roomId == joined.roomId)
  }

  it should "create a public room and join such room" in {
    val room = Await.result(client createPublicRoom(ROOM_TYPE_NAME, Set.empty), DefaultTimeout)
    assert(client.joinedRooms().exists(_.roomId==room.roomId))
  }

  it should "fail on joining an already joined room" in {
    val room = Await.result(client createPublicRoom(ROOM_TYPE_NAME, Set.empty), DefaultTimeout)
    assertThrows[Exception] {
      Await.result(client joinById room.roomId, DefaultTimeout)
    }
  }

  it should "show no property if no property is defined in the room" in {
    val room = Await.result(client createPublicRoom (ExampleRooms.noPropertyRoomType, Set.empty), DefaultTimeout)
    room.properties should have size 0
  }

  it should "show the correct default room properties when properties are not overridden" in {
    val room = Await.result(client createPublicRoom (ExampleRooms.myRoomType, Set.empty), DefaultTimeout)
    room.properties should have size 2
    room.properties should contain ("a", IntRoomPropertyValue(0))
    room.properties should contain ("b", StringRoomPropertyValue("abc"))
  }

  it should "show the correct room property values when properties are overridden" in {
    val properties = Set(RoomProperty("a", 1), RoomProperty("b", "qwe"))
    val room = Await.result(client createPublicRoom (ExampleRooms.myRoomType, properties), DefaultTimeout)
    room.properties should have size 2
    room.properties should contain ("a", IntRoomPropertyValue(1))
    room.properties should contain ("b", StringRoomPropertyValue("qwe"))
  }
}
