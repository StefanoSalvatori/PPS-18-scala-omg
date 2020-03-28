package client

import common.TestConfig
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import server.GameServer
import server.room.RoomStrategy

import scala.concurrent.Await
import scala.concurrent.duration._

class ClientSpec extends AnyFlatSpec
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll
  with TestConfig {

  private val serverAddress = "localhost"
  private val serverPort = CLIENT_SPEC_SERVER_PORT

  private val ROOM_TYPE_NAME: String = "test_room"
  private val SERVER_LAUNCH_AWAIT_TIME = 10 seconds
  private val SERVER_SHUTDOWN_AWAIT_TIME = 10 seconds

  private var gameServer: GameServer = _
  private var client: Client = _

  override def beforeAll(): Unit = {
    gameServer = GameServer(serverAddress, serverPort)

    gameServer.defineRoom(ROOM_TYPE_NAME, new RoomStrategy {
      override def onJoin(): Unit = {}
      override def onMessageReceived(): Unit = {}
      override def onLeave(): Unit = {}
      override def onCreate(): Unit = {}
    })

    Await.ready(gameServer.start(), SERVER_LAUNCH_AWAIT_TIME)
  }

  override def afterAll(): Unit =
    Await.ready(gameServer.shutdown(), SERVER_SHUTDOWN_AWAIT_TIME)

  behavior of "Client"

  before {
    client = Client(serverAddress, serverPort)
  }

  it should "start with no joined rooms" in {
    assert(client.joinedRooms isEmpty)
  }

  it should "create a public room and automatically join such room" in {
    client createPublicRoom ROOM_TYPE_NAME
    Thread sleep 3000
    client.joinedRooms.size shouldEqual 1
  }
}
