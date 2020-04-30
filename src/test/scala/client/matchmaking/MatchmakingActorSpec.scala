package client.matchmaking

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.testkit.{ImplicitSender, TestKit}
import client.matchmaking.MatchmakingActor.{JoinMatchmaking, LeaveMatchmaking}
import com.typesafe.config.ConfigFactory
import common.communication.CommunicationProtocol.MatchmakingInfo
import common.http.Routes
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import server.GameServer
import server.matchmaking.Matchmaker
import test_utils.{ExampleRooms, TestConfig}

import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}

import test_utils.ExampleRooms._

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

  private var matchmakerActor1: ActorRef = _
  private var matchmakerActor2: ActorRef = _

  private var gameServer: GameServer = _

  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

  before {
    matchmakerActor1 = system actorOf MatchmakingActor(ClosableRoomWithState.name, serverUri, "")
    matchmakerActor2 = system actorOf MatchmakingActor(ClosableRoomWithState.name, serverUri, "")

    gameServer = GameServer(serverAddress, serverPort)

    //dummy matchmaking strategy that only pairs two clients
    def matchmaker[T]: Matchmaker[T] = map => map.toList match {
      case c1 :: c2 :: _ => Some(Map(c1._1 -> 0, c2._1 -> 1))
      case _ => None
    }

    gameServer.defineRoomWithMatchmaking(
      ClosableRoomWithState.name,
      () => ExampleRooms.ClosableRoomWithState(),
      matchmaker)
    Await.ready(gameServer.start(), DefaultDuration)
  }

  after {
    Await.ready(gameServer.terminate(), ServerShutdownAwaitTime)

  }


  "MatchmakingActor" should {
    "join a matchmaking queue and return a MatchmakeTicket when the match is created" in {

      matchmakerActor1 ! JoinMatchmaking
      matchmakerActor2 ! JoinMatchmaking
      expectMsgPF() {
        case Success(res) =>
          assert(res.isInstanceOf[MatchmakingInfo])
        case Failure(ex) =>
          println(ex.toString)
      }
    }

    "leave a matchmaking queue" in {
      matchmakerActor1 ! JoinMatchmaking
      matchmakerActor1 ! LeaveMatchmaking
      expectMsgType[Any]
      matchmakerActor1 ! PoisonPill


      //this should never respond because the other actor left the matchmake
      matchmakerActor2 ! JoinMatchmaking
      expectNoMessage()

    }
  }

}
