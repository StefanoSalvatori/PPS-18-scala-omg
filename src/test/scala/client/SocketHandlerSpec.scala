package client

import akka.http.scaladsl.Http
import akka.http.scaladsl.testkit.ScalatestRouteTest
import client.room.SocketHandler
import com.typesafe.scalalogging.LazyLogging
import common.{HttpRequests, RoomJsonSupport, TestConfig}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import server.GameServer
import server.room.ServerRoom
import server.route_service.RouteService

import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}

class SocketHandlerSpec extends AnyFlatSpec
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll
  with TestConfig
  with ScalatestRouteTest
  with LazyLogging
  with RoomJsonSupport {

  private val TestType = "test"
  private val Address = "localhost"
  private val Port = SOCKET_HANDLER_SPEC_SERVER_PORT
  private val Uri = s"$Address:$Port"

  //game server that echoes messages received on a web socket connection
  private val server = GameServer.mock(Address, Port, RouteService.routeServiceWithEchoWebSocket())
  private val client = Client(Address, Port)
  before {
    server.defineRoom(TestType, ServerRoom(_))
    Await.ready(server.start(), 5 seconds)
  }

  behavior of "Socket handler"

  it should "open a web socket" in {
    val room = Await.result(client.createPublicRoom(TestType, ""), 5 seconds)
    val socketHandler = SocketHandler(Uri, room.roomId)
    Await.result(socketHandler.openSocket(), 5 seconds)
  }

  it should "send messages to the web socket" in {
    val room = Await.result(client.createPublicRoom(TestType, ""), 5 seconds)
    val socketHandler = SocketHandler(Uri, room.roomId)
    val p = Promise[Boolean]()
    socketHandler.onMessageReceived {
      case "test" => p.success(true)
      case _ => p.success(false)
    }
    Await.result(socketHandler.openSocket(), 5 seconds)
    socketHandler.sendMessage("test")
    assert(Await.result(p.future, 5 seconds))
  }

}
