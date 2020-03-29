package client

import akka.http.scaladsl.testkit.ScalatestRouteTest
import client.Client
import com.typesafe.scalalogging.{LazyLogging, Logger}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import server.GameServer
import server.room.ServerRoom.RoomStrategy

import scala.language.{implicitConversions, postfixOps}

class ClientSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest with BeforeAndAfter with BeforeAndAfterAll
with LazyLogging{



  private val serverAddress = "localhost"
  private val serverPort = 8080
  val client = Client(serverAddress, serverPort)
  val gameServer = GameServer(serverAddress, serverPort)


  behavior of "Client facade"

  override def beforeAll() {
    gameServer.defineRoom("test", RoomStrategy.empty)
    gameServer.onStart(logger debug "server started")
    gameServer.start()
  }

  before {
  }

  after {
  }

  it should "get available room of specific type" in {
    client.getAvailableRoomsByType("test")
  }
}
