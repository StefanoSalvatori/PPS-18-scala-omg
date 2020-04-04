package server

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, get, _}
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Sink
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import common.{Routes, TestConfig}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import server.ServerActor._
import server.utils.HttpRequestsActor
import server.utils.HttpRequestsActor.{Request, RequestFailed}

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
  private val REQUEST_FAIL_TIMEOUT: FiniteDuration = 20 seconds
  private val HTTP_BIND_TIMEOUT = 5 seconds
  private val ROUTES_BASE_PATH: String = "test"
  private val ROUTES: Route =
    path(ROUTES_BASE_PATH) {
      get {
        complete(StatusCodes.OK)
      }
    }

  private val serverActor: ActorRef = system actorOf ServerActor(SERVER_TERMINATION_DEADLINE, ROUTES)
  private val httpClientActor: ActorRef = system actorOf HttpRequestsActor()


  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A server actor" must {
    "fail to start the server if the port is already binded" in {
      val bind = Await.result(Http().bind(HOST, PORT).to(Sink.ignore).run(), HTTP_BIND_TIMEOUT)
      serverActor ! StartServer(HOST, PORT)
      expectMsgType[ServerFailure]
      Await.ready(bind.unbind(), HTTP_BIND_TIMEOUT)

    }

    "send a 'Started' response when StartServer is successful" in {
      serverActor ! StartServer(HOST, PORT)
      expectMsg(Started)
    }

    "send a ServerAlreadyRunning or ServerIsStarting message when StartServer is called multiple times before stop" in {
      serverActor ! StartServer(HOST, PORT)
      expectMsgAnyOf(ServerAlreadyRunning, ServerIsStarting)

    }

    "use the routes passed as parameters in the StartServer message" in {
      makeGetRequestAt(s"$ROUTES_BASE_PATH")
      val response = expectMsgType[HttpResponse]
      response.status shouldBe StatusCodes.OK
      response.discardEntityBytes()
    }


    "stop the server when StopServer is received" in {
      serverActor ! StopServer
      expectMsg(Stopped)

      // Await.ready(Http(system).shutdownAllConnectionPools(), MAX_WAIT_CONNECTION_POOL_SHUTDOWN)
      //Now requests fail
      makeGetRequestAt(s"$ROUTES_BASE_PATH")
      expectMsgType[RequestFailed](REQUEST_FAIL_TIMEOUT)
    }
  }

  private def makeGetRequestAt(path: String): Unit = {
    httpClientActor ! Request(HttpRequest(HttpMethods.GET, s"http://$HOST:$PORT/$path"))

  }

}
