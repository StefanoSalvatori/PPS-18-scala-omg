package client.matchmaking

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.testkit.{ImplicitSender, TestKit}
import client.matchmaking.ClientMatchmaker.MatchmakeTicket
import client.matchmaking.MatchmakingActor.{JoinMatchmake, LeaveMatchmake}
import client.{Client, CoreClient}
import client.room.{ClientRoom, ClientRoomActor}
import client.utils.MessageDictionary.{CreatePublicRoom, FailResponse, HttpRoomResponse}
import com.typesafe.config.ConfigFactory
import common.TestConfig
import common.communication.CommunicationProtocol.SessionId
import common.http.Routes
import common.room.Room.{RoomId, SharedRoom}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import server.GameServer
import server.matchmaking.MatchmakingService.{JoinQueue, Matchmaker}
import server.utils.ExampleRooms

import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success, Try}

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
    matchmakeActor1 = system actorOf MatchmakingActor(ExampleRooms.closableRoomWithStateType, serverUri)
    matchmakeActor2 = system actorOf MatchmakingActor(ExampleRooms.closableRoomWithStateType, serverUri)

    gameServer = GameServer(serverAddress, serverPort)

    //dummy matchmaking strategy that only pairs two clients
     def matchmakingStrategy: Matchmaker = {
      case c1 :: c2 :: _ => Some(Map(c1 -> 0, c2 -> 1))
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
    matchmakeActor1 ! PoisonPill
    matchmakeActor2 ! PoisonPill

  }

  "MatchmakingActor" should {
    "join a matchmaking queue and return a MatchmakeTicket when the match is created" in {

      matchmakeActor1 ! JoinMatchmake
      matchmakeActor2 ! JoinMatchmake
      expectMsgPF() {
        case Success(res) =>
          assert(res.isInstanceOf[MatchmakeTicket])
        case Failure(ex) =>
          println(ex.toString)
      }
    }

    "leave a matchmaking queue" in {

      matchmakeActor1 ! JoinMatchmake
      Thread.sleep(1000) //wait join complete
      matchmakeActor1 ! LeaveMatchmake
      expectMsgType[Any]


      //this should never respond beacause the other actor left the matchmake
      matchmakeActor2 ! JoinMatchmake

      expectNoMessage()

    }
  }

}
