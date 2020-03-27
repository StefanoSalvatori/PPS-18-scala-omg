package client

import java.util.concurrent.TimeUnit

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import server.GameServer
import server.room.RoomStrategy

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ClientSpec extends AnyFlatSpec
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll {

  private val serverAddress = "localhost"
  private val serverPort = 8080

  private val ROOM_TYPE_NAME: String = "test_room"
  private val SERVER_LAUNCH_AWAIT_TIME = 10 // sec

  private var gameServer: GameServer = _
  private var client: Client = _

  override def beforeAll(): Unit = {
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

  override def afterAll(): Unit = gameServer.shutdown()

  behavior of "Client"

  before {
    client = Client(serverAddress, serverPort)
  }

  it should "start with no joined rooms" in {
    assert(client.joinedRooms isEmpty)
  }

  it should "create a public room and automatically join such room" in {
    client.createPublicRoom()
    Thread sleep 3000
    client.joinedRooms.size shouldEqual 1
  }
}
