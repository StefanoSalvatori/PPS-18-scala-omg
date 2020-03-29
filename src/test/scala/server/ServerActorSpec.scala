package server

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, get, _}
import akka.http.scaladsl.server.Route
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import common.TestConfig
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import server.ServerActor._
import server.utils.HttpRequestsActor
import server.utils.HttpRequestsActor.Request

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps}


class ServerActorSpec extends TestKit(ActorSystem("ServerSystem", ConfigFactory.load()))
  with ImplicitSender
  with Matchers
  with AnyWordSpecLike
  with BeforeAndAfter
  with BeforeAndAfterAll
  with TestConfig {

  private val HOST: String = "localhost"
  private val PORT: Int = SERVER_ACTOR_SPEC_PORT
  private val SERVER_TERMINATION_DEADLINE: FiniteDuration = 2 seconds
  private val ROUTES_BASE_PATH: String = "test"
  private val ROUTES: Route =
    path(ROUTES_BASE_PATH) {
      get {
        complete(StatusCodes.OK)
      }
    }

  private val serverActor: ActorRef = system actorOf ServerActor(SERVER_TERMINATION_DEADLINE)
  private val httpClientActor: ActorRef = system actorOf HttpRequestsActor()


  before {
  }
  after {

  }

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  override def beforeAll(): Unit = {

  }

  "A server actor" must {
    "start a server at the specified host and port when receives StartServer message" in {
      serverActor ! StartServer(HOST, PORT, ROUTES)
      expectMsg(Started)
    }

    "send a ServerAlreadyRunning or ServerIsStarting message when StartServer is called multiple times before stop" in {
      serverActor ! StartServer(HOST, PORT, ROUTES)
      expectMsgAnyOf(ServerAlreadyRunning, ServerIsStarting)

    }

    "use the routes passed as parameters in the StartServer message" in {
      makeGetRequestAt(s"$ROUTES_BASE_PATH")
      val response = expectMsgType[HttpResponse]
      response.status shouldBe StatusCodes.OK
      response.discardEntityBytes()
    }

    "stop the server when StopServer is received waiting the server termination deadline before closing existing " +
      "connections" in {
      serverActor ! StopServer
      expectMsg(Stopped)

      //Even if the server is stopped, connections already opened are still active
      makeGetRequestAt(s"$ROUTES_BASE_PATH")
      val response = expectMsgType[HttpResponse]
      response.status shouldBe StatusCodes.OK
      response.discardEntityBytes()

      Await.ready(Http(system).shutdownAllConnectionPools(), 10 seconds)

      //Wait that the termination deadline is reached
      Thread.sleep(SERVER_TERMINATION_DEADLINE.toMillis)

      //Now requests fail
      makeGetRequestAt(s"$ROUTES_BASE_PATH")
      expectNoMessage()

    }


  }

  private def makeGetRequestAt(path: String): Unit = {
    httpClientActor ! Request(HttpRequest(HttpMethods.GET, s"http://$HOST:$PORT/$path"))

  }

}
