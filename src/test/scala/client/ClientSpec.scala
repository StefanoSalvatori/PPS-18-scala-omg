package client

import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestKit
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import common.room.{FilterOptions, RoomJsonSupport, RoomProperty}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import server.GameServer
import server.room.ServerRoom
import common.room.RoomPropertyValueConversions._
import test_utils.ExampleRooms._
import test_utils.TestConfig

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

  private val serverAddress = "localhost"
  private val serverPort = ClientSpecServerPort

  private val RoomTypeName: String = "test_room"

  implicit val execContext: ExecutionContextExecutor = system.dispatcher
  private var gameServer: GameServer = _
  private var client: Client = _
  private var client2: Client = _

  behavior of "Client"

  val testProperties = Set(RoomProperty("a", 1), RoomProperty("b", "qwe"))

  implicit val timeoutToDuration: Timeout => Duration = timeout => timeout.duration

  before {
    gameServer = GameServer(serverAddress, serverPort)
    gameServer.defineRoom(RoomTypeName, () => ServerRoom())
    gameServer.defineRoom(RoomWithProperty.name, RoomWithProperty.apply)
    gameServer.defineRoom(NoPropertyRoom.name, NoPropertyRoom.apply)
    gameServer.defineRoom(ClosableRoomWithState.name, ClosableRoomWithState.apply)
    gameServer.defineRoom(RoomWithReconnection.name, RoomWithReconnection.apply)

    Await.ready(gameServer.start(), ServerLaunchAwaitTime)
    logger debug s"Server started at $serverAddress:$serverPort"

    client = Client(serverAddress, serverPort)
    client2 = Client(serverAddress, serverPort)
  }

  after {
    Await.ready(gameServer.terminate(), ServerShutdownAwaitTime)
  }

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  it should "start with no joined rooms" in {
    client.joinedRooms shouldBe empty
  }

  it should "create public rooms" in {
    val roomList1 = Await.result(client getAvailableRoomsByType(RoomTypeName, FilterOptions.empty), DefaultTimeout)
    roomList1 should have size 0
    Await.ready(client createPublicRoom RoomTypeName, DefaultTimeout)
    val roomList2 = Await.result(client getAvailableRoomsByType(RoomTypeName, FilterOptions.empty), DefaultTimeout)
    roomList2 should have size 1
  }

  it should "create public room and get such rooms asking the available rooms" in {
    Await.ready(client createPublicRoom(RoomTypeName, Set.empty), DefaultTimeout)
    val roomList = Await.result(client getAvailableRoomsByType(RoomTypeName, FilterOptions.empty), DefaultTimeout)
    roomList should have size 1
  }

  it should "fail on create public rooms if server is unreachable" in {
    Await.ready(gameServer.stop(), ServerShutdownAwaitTime)

    assertThrows[Exception] {
      Await.result(client createPublicRoom RoomTypeName, DefaultTimeout)
    }
  }

  it should "fail on getting rooms  if server is stopped" in {
    Await.ready(gameServer.stop(), ServerShutdownAwaitTime)
    assertThrows[Exception] {
      Await.result(client getAvailableRoomsByType(RoomTypeName, FilterOptions.empty), DefaultTimeout)
    }
  }

  it should "get all available rooms of specific type" in {
    Await.ready(client createPublicRoom RoomTypeName, DefaultTimeout)
    Await.ready(client createPublicRoom RoomTypeName, DefaultTimeout)
    val roomList = Await.result(client getAvailableRoomsByType(RoomTypeName, FilterOptions.empty), DefaultTimeout)
    roomList should have size 2
  }

  it should "fail on joining a room if server is unreachable" in {
    Await.ready(client createPublicRoom RoomTypeName, DefaultTimeout)
    Await.ready(gameServer.stop(), ServerShutdownAwaitTime)

    assertThrows[Exception] {
      Await.result(client join(RoomTypeName, FilterOptions.empty), DefaultTimeout)
    }
  }

  it should "fail on joining a room of a type that does not exists" in {
    assertThrows[Exception] {
      Await.result(client join(RoomTypeName, FilterOptions.empty), DefaultTimeout)
    }
  }

  it should "be able to join a room by it's id" in {
    val room1 = Await.result(client.createPublicRoom(RoomTypeName, Set.empty), DefaultTimeout)
    val room2 = Await.result(client2.joinById(room1.roomId), DefaultTimeout)
    val joinedRooms = client2 joinedRooms()
    joinedRooms should have size 1
    room2.roomId shouldEqual room1.roomId
  }

  it should "join a room or create it if it does not exists" in {
    val room = Await.result(client.joinOrCreate(RoomTypeName, FilterOptions.empty), DefaultTimeout)
    client joinedRooms() should have size 1
    val roomOnServer = Await.result(client.getAvailableRoomsByType(RoomTypeName, FilterOptions.empty), DefaultTimeout)
    assert(roomOnServer.map(_.roomId).contains(room.roomId))
  }

  it should "be able to join an existing room" in {
    val room = Await.result(client createPublicRoom RoomTypeName, DefaultTimeout)
    val joined = Await.result(client join(RoomTypeName, FilterOptions.empty), DefaultTimeout)
    assert(room.roomId == joined.roomId)
  }

  it should "create a public room and join such room" in {
    val room = Await.result(client createPublicRoom RoomTypeName, DefaultTimeout)
    assert(client.joinedRooms().exists(_.roomId == room.roomId))
  }

  it should "fail on joining an already joined room" in {
    val room = Await.result(client createPublicRoom RoomTypeName, DefaultTimeout)
    assertThrows[Exception] {
      Await.result(client joinById room.roomId, DefaultTimeout)
    }
  }

  it should "not create a room if an available room exists " in {
    Await.result(client.createPublicRoom(RoomTypeName), DefaultTimeout)
    Await.result(client2.joinOrCreate(RoomTypeName, FilterOptions.empty), DefaultTimeout)
    val roomsOnServer = Await.result(client.getAvailableRoomsByType(RoomTypeName, FilterOptions.empty), DefaultTimeout)
    roomsOnServer should have size 1
  }

  it should "join a private room if the right password is provided" in {
    val room = Await.result(client.createPrivateRoom(RoomTypeName, password = "pwd"), DefaultTimeout)
    val res = Await.result(client2.joinById(room.roomId, "pwd"), DefaultTimeout)
    assert(res == room)
  }

  it should "not join a private room if a wrong password is provided" in {
    val room = Await.result(client.createPrivateRoom(RoomTypeName, password = "pwd"), DefaultTimeout)
    assertThrows[Exception] {
      Await.result(client2.joinById(room.roomId, "pwd2"), DefaultTimeout)
    }
  }

  it should "leave rooms" in {
    val room = Await.result(client.joinOrCreate(RoomTypeName, FilterOptions.empty, Set.empty), DefaultTimeout)
    Await.result(room.leave(), DefaultTimeout)
  }

  it should "allow to reconnect to a previously joined room (that allows reconnection) with the same session id" in {
    val room = Await.result(client.joinOrCreate(RoomWithReconnection.name, FilterOptions.empty, Set.empty), DefaultTimeout)
    Await.result(room.leave(), DefaultTimeout)
    val res = Await.result(client.reconnect(room.roomId, room.sessionId), DefaultTimeout)

    room.sessionId shouldEqual res.sessionId

  }

  it should "not allow to reconnect an already joined room" in {
    val room = Await.result(client.joinOrCreate(RoomWithReconnection.name, FilterOptions.empty, Set.empty), DefaultTimeout)

    assertThrows[Exception] {
      Await.result(client.reconnect(room.roomId, room.sessionId), DefaultTimeout)
    }
  }

  it should "try to join all rooms available until success" in {
    val testProperty = RoomProperty("a", 1)
    val testProperty2 = RoomProperty("a", 2)
    val testProperty3 = RoomProperty("a", 3)

    //create 3 rooms so that only the second one matches the filters
    Await.ready(client.createPublicRoom(RoomWithProperty.name, Set(testProperty)), DefaultTimeout)
    val room = Await.result(client.createPublicRoom(RoomWithProperty.name, Set(testProperty3)), DefaultTimeout)
    Await.ready(client.createPublicRoom(RoomWithProperty.name, Set(testProperty2)), DefaultTimeout)

    val joined = Await.result(client.join(RoomWithProperty.name, FilterOptions just testProperty =:= 3), DefaultTimeout)
    room.roomId shouldEqual joined.roomId
  }
}
