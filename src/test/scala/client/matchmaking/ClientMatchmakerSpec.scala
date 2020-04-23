package client.matchmaking

import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestKit
import akka.util.Timeout
import client.CoreClient
import client.room.JoinedRoom
import com.typesafe.scalalogging.LazyLogging
import common.TestConfig
import common.http.Routes
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import server.GameServer
import server.matchmaking.MatchmakingService.Matchmaker
import server.utils.ExampleRooms

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor}


class ClientMatchmakerSpec extends AnyWordSpecLike
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll
  with TestConfig
  with ScalatestRouteTest
  with LazyLogging {


  private val serverAddress = "localhost"
  private val serverPort = ClientMatchmakingSpecServerPort
  private val serverUri = Routes.httpUri(serverAddress, serverPort)

  implicit val execContext: ExecutionContextExecutor = system.dispatcher
  private var gameServer: GameServer = _
  private var matchmaker1: ClientMatchmaker = _
  private var matchmaker2: ClientMatchmaker = _


  implicit val timeoutToDuration: Timeout => Duration = timeout => timeout.duration

  before {
    gameServer = GameServer(serverAddress, serverPort)

    //dummy matchmaking strategy that only consider one client
    def matchmakingStrategy: Matchmaker = {
      case c1 ::  _ => Some(Map(c1 -> 0))
      case _ => None
    }

    gameServer.defineRoomWithMatchmaking(
      ExampleRooms.closableRoomWithStateType,
      () => ExampleRooms.ClosableRoomWithState(),
      matchmakingStrategy)

    Await.ready(gameServer.start(), ServerLaunchAwaitTime)

    matchmaker1 = ClientMatchmaker(system actorOf CoreClient(serverUri), serverUri)
    matchmaker2 = ClientMatchmaker(system actorOf CoreClient(serverUri), serverUri)

  }

  after {
    Await.ready(gameServer.terminate(), ServerShutdownAwaitTime)
  }

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  /*"Client Matchmaker" should {
    "join a match making queue for a given type" in {
      val room = Await.result(this.matchmaker1.joinMatchmake(ExampleRooms.closableRoomWithStateType), DefaultTimeout)
      assert(room.isInstanceOf[JoinedRoom])
    }
  }*/


}