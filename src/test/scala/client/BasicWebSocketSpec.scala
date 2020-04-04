package client

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.http.scaladsl.server.Directives.{get, handleWebSocketMessages, _}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import akka.stream.scaladsl.Flow
import client.room.{RoomSocket, WebSocket}
import com.typesafe.scalalogging.LazyLogging
import common.{RoomJsonSupport, Routes, TestConfig}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import server.GameServer
import server.room.ServerRoom
import server.route_service.RouteService

import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}

class BasicWebSocketSpec extends AnyFlatSpec
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll
  with TestConfig
  with ScalatestRouteTest
  with LazyLogging
  with RoomJsonSupport {

  //TODO: test
  //simple web socket that echoes messages
  /*private val echoWebSocketRoute: Route =
    path("testroute") {
      get {
        handleWebSocketMessages(Flow[Message].map(x => x))
      }
    }

  private val TestType = "test"
  private val Address = "localhost"
  private val Port = SOCKET_HANDLER_SPEC_SERVER_PORT
  private val Uri = s"$Address:$Port"
  private val WebSocketRoute = s"ws://$Uri/testroute"

  private var server = GameServer(Address, Port, echoWebSocketRoute)

  before {
    server = GameServer(Address, Port, echoWebSocketRoute)
    server.defineRoom(TestType, ServerRoom(_))
    Await.ready(server.start(), 5 seconds)
  }

  after {
    Await.ready(server.shutdown(), 5 seconds)
  }

  behavior of "Socket handler"


  it should "open a web socket" in {
    val socketHandler = WebSocket(WebSocketRoute)
    Await.result(socketHandler.openSocket(), 5 seconds)
  }

  it should "send messages to the web socket" in {
    val socketHandler = WebSocket(WebSocketRoute)
    val p = Promise[Boolean]()
    socketHandler.onMessageReceived {
      case "test" => p.success(true)
      case _ => p.success(false)
    }
    Await.result(socketHandler.openSocket(), 5 seconds)
    socketHandler.sendMessage("test")
    assert(Await.result(p.future, 5 seconds))
  }*/

}
