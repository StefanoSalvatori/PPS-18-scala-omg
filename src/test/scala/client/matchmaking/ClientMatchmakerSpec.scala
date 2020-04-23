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
import server.matchmaking.MatchmakingService.MatchmakingStrategy
import server.utils.ExampleRooms

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor, Promise}


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
    def matchmakingStrategy: MatchmakingStrategy = map => map.toList match {
      case c1 :: c2 :: _ => Some(Map(c1._1 -> 0, c2._1 -> 1))
      case _ => None
    }

    gameServer.defineRoomWithMatchmaking(
      ExampleRooms.closableRoomWithStateType,
      () => ExampleRooms.ClosableRoomWithState(),
      matchmakingStrategy)

    gameServer.defineRoomWithMatchmaking(
      ExampleRooms.roomWithPropertyType,
      () => ExampleRooms.ClosableRoomWithState(),
      matchmakingStrategy)

    Await.ready(gameServer.start(), ServerLaunchAwaitTime)

    //simulate to clients that can join or leave the matchmaking
    matchmaker1 = ClientMatchmaker(system actorOf CoreClient(serverUri), serverUri)
    matchmaker2 = ClientMatchmaker(system actorOf CoreClient(serverUri), serverUri)

  }

  after {
    Await.ready(gameServer.terminate(), ServerShutdownAwaitTime)
  }

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "Client Matchmaker" should {
    "join a match making queue for a given type" in {
      this.matchmaker1.joinMatchmake(ExampleRooms.closableRoomWithStateType)
      val room = Await.result(this.matchmaker2.joinMatchmake(ExampleRooms.closableRoomWithStateType), DefaultTimeout)
      assert(room.isInstanceOf[JoinedRoom])
    }

    "fail the join future when leaving the matchmaking queue" in {
      val p = Promise[JoinedRoom]()
      p.completeWith(this.matchmaker1.joinMatchmake(ExampleRooms.closableRoomWithStateType))

      Await.result(this.matchmaker1.leaveMatchmake(ExampleRooms.closableRoomWithStateType), DefaultTimeout)
      assert(p.isCompleted)
    }

    "handle multiple matchmake requests" in {
      //matchmake request 1
      this.matchmaker2.joinMatchmake(ExampleRooms.closableRoomWithStateType)
      val room = Await.result(this.matchmaker1.joinMatchmake(ExampleRooms.closableRoomWithStateType), DefaultTimeout)
      assert(room.isInstanceOf[JoinedRoom])

      //matchmake request 2
      this.matchmaker2.joinMatchmake(ExampleRooms.roomWithPropertyType)
      val room2 = Await.result(this.matchmaker1.joinMatchmake(ExampleRooms.roomWithPropertyType), DefaultTimeout)
      assert(room2.isInstanceOf[JoinedRoom])

    }

    "return the same future on multiple requests on the same room type" in {
      this.matchmaker2.joinMatchmake(ExampleRooms.roomWithPropertyType)

      this.matchmaker1.joinMatchmake(ExampleRooms.roomWithPropertyType)
      val room2 = Await.result(this.matchmaker1.joinMatchmake(ExampleRooms.roomWithPropertyType), DefaultTimeout)
      assert(room2.isInstanceOf[JoinedRoom])
    }

    "successfully completes when leaving a matchmake that was never joined" in {
      Await.result(this.matchmaker1.leaveMatchmake(ExampleRooms.roomWithPropertyType), DefaultTimeout)
    }
  }


}