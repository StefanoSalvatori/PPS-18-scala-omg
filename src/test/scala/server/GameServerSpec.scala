package server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, get, _}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.Sink
import akka.testkit.TestKit
import common.http.{HttpRequests, Routes}
import common.room.Room.SharedRoom
import common.room.{FilterOptions, RoomJsonSupport}
import common.TestConfig
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import server.room.ServerRoom

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.{implicitConversions, postfixOps}

class GameServerSpec extends AnyFlatSpec
  with Matchers
  with ScalatestRouteTest
  with BeforeAndAfter
  with RoomJsonSupport
  with TestConfig {

  implicit val actorSystem: ActorSystem = ActorSystem()

  private val MAX_WAIT_REQUESTS = 5 seconds
  private val MAX_WAIT_CONNECTION_POOL_SHUTDOWN = 15 seconds

  private val MAX_WAIT_SERVER_STARTUP = 5 seconds
  private val MAX_WAIT_SERVER_SHUTDOWN = 5 seconds

  private val HOST: String = "localhost"
  private val PORT: Int = GAMESERVER_SPEC_SERVER_PORT

  private val ADDITIONAL_PATH = "test"
  private val ADDITIONAL_TEST_ROUTES =
    path(ADDITIONAL_PATH) {
      get {
        complete(StatusCodes.OK)
      }
    }

  private val server: GameServer = GameServer(HOST, PORT, ADDITIONAL_TEST_ROUTES)

  behavior of "Game Server facade"


  after {
    Await.result(Http().shutdownAllConnectionPools(), MAX_WAIT_CONNECTION_POOL_SHUTDOWN)
  }


  override def afterAll(): Unit = {
    Await.ready(this.server.terminate(), MAX_WAIT_SERVER_SHUTDOWN)
    TestKit.shutdownActorSystem(system)
  }

  it should "allow the creation of a server with a specified address and port" in {
    assert(this.server.host equals HOST)
    assert(this.server.port equals PORT)
  }


  it should "not accept requests before start() is called" in {
    assertThrows[Exception] {
      Await.result(this.makeEmptyRequest(), MAX_WAIT_REQUESTS)
    }
  }

  it should "throw an exception if we call start() but the port we want to use is already binded" in {
    val bind = Await.result(Http(actorSystem).bind(HOST, PORT).to(Sink.ignore).run(), MAX_WAIT_CONNECTION_POOL_SHUTDOWN)
    assertThrows[Exception] {
      Await.result(server.start(), MAX_WAIT_SERVER_STARTUP)
    }
    Await.ready(bind.unbind(), MAX_WAIT_CONNECTION_POOL_SHUTDOWN)
  }


  it should "allow to start a server listening for http requests" in {
    Await.result(this.server.start(), MAX_WAIT_SERVER_STARTUP)
    val res = Await.result(this.makeEmptyRequest(), MAX_WAIT_SERVER_STARTUP)
    assert(res.isResponse())
    res.discardEntityBytes()
    Await.result(this.server.stop(), MAX_WAIT_SERVER_SHUTDOWN)

  }


  it should "stop the server when stop() is called" in {
    Await.result(this.server.start(), MAX_WAIT_SERVER_STARTUP)
    Await.result(this.server.stop(), MAX_WAIT_SERVER_SHUTDOWN)
    assertThrows[Exception] {
      Await.result(this.makeEmptyRequestAtRooms, MAX_WAIT_REQUESTS)
    }
  }

  it should "throw an IllegalStateException when stop() is called before start()" in {
    assertThrows[IllegalStateException] {
      Await.result(this.server.stop(), MAX_WAIT_SERVER_SHUTDOWN)
    }
  }

  it should s"respond to requests received at ${Routes.rooms}" in {
    Await.result(this.server.start(), MAX_WAIT_SERVER_STARTUP)
    val response = Await.result(this.makeEmptyRequestAtRooms, MAX_WAIT_REQUESTS)
    assert(response.status equals StatusCodes.OK)
    response.discardEntityBytes()
    Await.result(this.server.stop(), MAX_WAIT_SERVER_SHUTDOWN)

  }

  it should "restart calling start() after stop()" in {
    Await.result(this.server.start(), MAX_WAIT_SERVER_STARTUP)
    Await.result(this.server.stop(), MAX_WAIT_SERVER_SHUTDOWN)
    Await.result(this.server.start(), MAX_WAIT_SERVER_STARTUP)
    val res = Await.result(this.makeEmptyRequest(), MAX_WAIT_SERVER_STARTUP)
    assert(res.isResponse())

    Await.result(this.server.stop(), MAX_WAIT_SERVER_SHUTDOWN)


  }

  it should "throw an IllegalStateException if start() is called twice" in {
    Await.result(this.server.start(), MAX_WAIT_SERVER_STARTUP)
    assertThrows[IllegalStateException] {
      Await.result(this.server.start(), MAX_WAIT_SERVER_STARTUP)
    }
    Await.result(this.server.stop(), MAX_WAIT_SERVER_STARTUP)

  }

  it should "allow to specify behaviour during stop" in {
    Await.result(this.server.start(), MAX_WAIT_SERVER_STARTUP)
    var flag = false
    this.server.onStop {
      flag = true
    }
    Await.result(this.server.stop(), MAX_WAIT_SERVER_STARTUP)
    flag shouldBe true
  }

  it should "allow to specify behaviour during startup" in {
    var flag = false
    this.server.onStart {
      flag = true
    }
    Await.result(this.server.start(), MAX_WAIT_SERVER_STARTUP)
    flag shouldBe true

    Await.result(this.server.stop(), MAX_WAIT_SERVER_SHUTDOWN)

  }

  it should "use the additional routes passed as parameter" in {
    Await.result(this.server.start(), MAX_WAIT_SERVER_STARTUP)
    val response = Await.result(this.makeEmptyRequestAtPath(ADDITIONAL_PATH), MAX_WAIT_REQUESTS)
    response.status shouldBe StatusCodes.OK
    response.discardEntityBytes()
    Await.result(this.server.stop(), MAX_WAIT_SERVER_SHUTDOWN)

  }

  it should "create rooms" in {
    this.server.defineRoom("test", () => ServerRoom())
    Await.result(this.server.start(), MAX_WAIT_SERVER_STARTUP)
    this.server.createRoom("test")
    val httpResult = Await.result(makeEmptyRequestAtRooms, MAX_WAIT_REQUESTS)
    val roomsList = Await.result(Unmarshal(httpResult).to[Seq[SharedRoom]], MAX_WAIT_REQUESTS)
    roomsList should have size 1
    Await.result(this.server.stop(), MAX_WAIT_SERVER_STARTUP)


  }


  private def makeEmptyRequest(): Future[HttpResponse] = {
    this.makeEmptyRequestAtRooms
  }

  private def makeEmptyRequestAtRooms: Future[HttpResponse] = {
    Http().singleRequest(HttpRequests.getRooms(Routes.httpUri(HOST, PORT))(FilterOptions.empty))
  }

  private def makeEmptyRequestAtRoomsWithType(roomType: String): Future[HttpResponse] = {
    Http().singleRequest(HttpRequests.getRoomsByType(Routes.httpUri(HOST, PORT))(roomType, FilterOptions.empty))
  }

  private def makeEmptyRequestAtPath(path: String): Future[HttpResponse] = {
    Http().singleRequest(HttpRequest(uri = Routes.httpUri(HOST,PORT) + "/" + path))
  }
}
