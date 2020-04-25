package client.matchmaking

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.testkit.{ImplicitSender, TestKit}
import client.matchmaking.MatchmakingActor.{JoinMatchmaking, LeaveMatchmaking}
import com.typesafe.config.ConfigFactory
import common.TestConfig
import common.communication.CommunicationProtocol.MatchmakingInfo
import common.http.Routes
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import server.GameServer
import server.matchmaking.MatchmakingService.MatchmakingStrategy
import server.utils.ExampleRooms

import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}

class MatchmakingActorSpec extends TestKit(ActorSystem("ClientSystem", ConfigFactory.load()))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll
  with TestConfig {

  implicit val executionContext: ExecutionContext = system.dispatcher
  private val serverAddress = "localhost"
  private val serverPort = MatchmakingSpecServerPort
  private val serverUri = Routes.httpUri(serverAddress, serverPort)

  private var matchmakeActor1: ActorRef = _
  private var matchmakeActor2: ActorRef = _

  private var gameServer: GameServer = _

  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

  before {
    matchmakeActor1 = system actorOf MatchmakingActor(ExampleRooms.closableRoomWithStateType, serverUri, "")
    matchmakeActor2 = system actorOf MatchmakingActor(ExampleRooms.closableRoomWithStateType, serverUri, "")

    gameServer = GameServer(serverAddress, serverPort)

    //dummy matchmaking strategy that only pairs two clients
    def matchmakingStrategy: MatchmakingStrategy = map => map.toList match {
      case c1 :: c2 :: _ => Some(Map(c1._1 -> 0, c2._1 -> 1))
      case _ => None
    }

    gameServer.defineRoomWithMatchmaking(
      ExampleRooms.closableRoomWithStateType,
      () => ExampleRooms.ClosableRoomWithState(),
      matchmakingStrategy)
    Await.ready(gameServer.start(), DefaultDuration)
  }

  after {
    Await.ready(gameServer.terminate(), ServerShutdownAwaitTime)

  }


  "MatchmakingActor" should {
    "join a matchmaking queue and return a MatchmakeTicket when the match is created" in {

      matchmakeActor1 ! JoinMatchmaking
      matchmakeActor2 ! JoinMatchmaking
      expectMsgPF() {
        case Success(res) =>
          assert(res.isInstanceOf[MatchmakingInfo])
        case Failure(ex) =>
          println(ex.toString)
      }
    }

    "leave a matchmaking queue" in {
      matchmakeActor1 ! JoinMatchmaking
      matchmakeActor1 ! LeaveMatchmaking
      expectMsgType[Any]
      matchmakeActor1 ! PoisonPill


      //this should never respond because the other actor left the matchmake
      matchmakeActor2 ! JoinMatchmaking
      expectNoMessage()

    }
  }

}
