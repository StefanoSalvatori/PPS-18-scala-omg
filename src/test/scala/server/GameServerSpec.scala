package server

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.language.{implicitConversions, postfixOps}

class GameServerSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest with BeforeAndAfter with
  GameServerRoutes {

  private val MAX_WAIT_REQUESTS = 5 seconds
  private val MAX_WAIT_CONNECTION_POOL_SHUTDOWN = 15 seconds

  private val MAX_WAIT_SERVER_STARTUP = 5 seconds
  private val MAX_WAIT_SERVER_SHUTDOWN = 5 seconds

  private val HOST: String = "localhost"
  private val PORT: Int = 8080

  implicit val execContext: ExecutionContextExecutor = system.dispatcher
  private var server: GameServer = _

  behavior of "Game Server facade"

  before {
    this.server = GameServer(HOST, PORT)
  }

  after {
    if (this.server.isStarted) {
      Await.result(this.server.shutdown(), MAX_WAIT_SERVER_SHUTDOWN)
    }
    Await.result(Http().shutdownAllConnectionPools(), MAX_WAIT_CONNECTION_POOL_SHUTDOWN)
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

  it should "allow to start a server listening for http requests" in {
    Await.result(this.server.start(), MAX_WAIT_SERVER_STARTUP)
    val res = Await.result(this.makeEmptyRequest(), MAX_WAIT_SERVER_STARTUP)
    assert(res.isResponse())
  }


  it should "stop the server when shutdown() is called" in {
    Await.result(this.server.start(), MAX_WAIT_SERVER_STARTUP)
    Await.result(this.server.shutdown(), MAX_WAIT_SERVER_SHUTDOWN)
    assertThrows[Exception] {
      Await.result(this.makeEmptyRequestAt(BASE_PATH), MAX_WAIT_REQUESTS)
    }
  }

  it should "throw an IllegalStateException when shutdown() is called before start()" in {
    assertThrows[IllegalStateException] {
      Await.result(this.server.shutdown(), MAX_WAIT_SERVER_SHUTDOWN)
    }
  }

  it should s"respond to requests received at $BASE_PATH" in {
    Await.result(this.server.start(), MAX_WAIT_SERVER_STARTUP)
    val response = Await.result(this.makeEmptyRequestAt(BASE_PATH), MAX_WAIT_REQUESTS)
    assert(response.status equals StatusCodes.OK)
  }

  it should "restart calling start() after shutdown()" in {
    Await.result(this.server.start(), MAX_WAIT_SERVER_STARTUP)
    Await.result(this.server.shutdown(), MAX_WAIT_SERVER_SHUTDOWN)
    Await.result(this.server.start(), MAX_WAIT_SERVER_STARTUP)
    val res = Await.result(this.makeEmptyRequest(), MAX_WAIT_SERVER_STARTUP)
    assert(res.isResponse())
  }

  it should "throw an IllegalStateException if start() is called twice" in {
    Await.result(this.server.start(), MAX_WAIT_SERVER_STARTUP)
    assertThrows[IllegalStateException]{
      Await.result(this.server.start(), MAX_WAIT_SERVER_STARTUP)
    }
  }

  it should "allow to specify behaviour during shutdown" in {
    Await.result(this.server.start(), MAX_WAIT_SERVER_STARTUP)
    var flag = false
    this.server.onShutdown {
      flag = true
    }
    Await.result(this.server.shutdown(), MAX_WAIT_SERVER_STARTUP)
    flag shouldBe true
  }

  it should "allow to specify behaviour during startup" in {
    var flag = false
    this.server.onStart {
      flag = true
    }
    Await.result(this.server.start(), MAX_WAIT_SERVER_STARTUP)
    flag shouldBe true
  }


  private def makeEmptyRequest(): Future[HttpResponse] = {
    this.makeEmptyRequestAt("")
  }

  private def makeEmptyRequestAt(path: String): Future[HttpResponse] = {
    Http().singleRequest(HttpRequest(uri = s"http://$HOST:$PORT/$path"))
  }

}
