package client.matchmaking

import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestKit
import akka.util.Timeout
import client.CoreClient
import client.room.JoinedRoom
import com.typesafe.scalalogging.LazyLogging
import common.TestConfig
import common.http.Routes
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import server.GameServer
import server.matchmaking.MatchmakingService.MatchmakingStrategy
import server.room.ServerRoom

import scala.concurrent.duration.{Duration, _}
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
  private val RoomType1 = "test1"
  private val RoomType2 = "test2"
  private val RoomType3 = "test3"




  before {
    gameServer = GameServer(serverAddress, serverPort)

    //dummy matchmaking strategy that only consider one client
    def matchmakingStrategy: MatchmakingStrategy = map => map.toList match {
      case c1 :: c2 :: _ => Some(Map(c1._1 -> 0, c2._1 -> 1))
      case _ => None
    }

    //matchmaking strategy that match client that have the same info
    def matchmakingEqualStrategy: MatchmakingStrategy = map => map.toList match {
      case (c1, c1Info) :: (c2, c2Info) :: _ if c1Info.equals(c2Info) => Some(Map(c1 -> 0, c2 -> 1))
      case _ => None
    }

    gameServer.defineRoomWithMatchmaking(RoomType1,  () => ServerRoom(),  matchmakingStrategy)
    gameServer.defineRoomWithMatchmaking(RoomType2,  () => ServerRoom(), matchmakingStrategy)
    gameServer.defineRoomWithMatchmaking(RoomType3, () => ServerRoom(), matchmakingEqualStrategy)

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
      this.matchmaker1.joinMatchmaking(RoomType1)
      val room = Await.result(this.matchmaker2.joinMatchmaking(RoomType1), DefaultTimeout)
      assert(room.isInstanceOf[JoinedRoom])
    }

    "fail the join future when leaving the matchmaking queue" in {
      val p = Promise[JoinedRoom]()
      p.completeWith(this.matchmaker1.joinMatchmaking(RoomType1))

      Await.result(this.matchmaker1.leaveMatchmaking(RoomType1), DefaultTimeout)
      assert(p.isCompleted)
    }

    "handle multiple matchmake requests" in {
      //matchmake request 1
      this.matchmaker2.joinMatchmaking(RoomType1)
      val room = Await.result(this.matchmaker1.joinMatchmaking(RoomType1), DefaultTimeout)
      assert(room.isInstanceOf[JoinedRoom])

      //matchmake request 2
      this.matchmaker2.joinMatchmaking(RoomType2)
      val room2 = Await.result(this.matchmaker1.joinMatchmaking(RoomType2), DefaultTimeout)
      assert(room2.isInstanceOf[JoinedRoom])

    }

    "return the same future on multiple requests on the same room type" in {
      this.matchmaker2.joinMatchmaking(RoomType2)

      this.matchmaker1.joinMatchmaking(RoomType2)
      val room2 = Await.result(this.matchmaker1.joinMatchmaking(RoomType2), DefaultTimeout)
      assert(room2.isInstanceOf[JoinedRoom])
    }

    "successfully completes when leaving a matchmake that was never joined" in {
      Await.result(this.matchmaker1.leaveMatchmaking(RoomType2), DefaultTimeout)
    }

    "fail if the matchmaking time exceeds the specified time" in {
      assertThrows[Exception] {
        Await.result(this.matchmaker1.joinMatchmaking(RoomType2, 5 seconds), DefaultTimeout)
      }
    }

    "send client info to the matchmaker" in {
      val clientInfo = ClientInfo(1)
      this.matchmaker1.joinMatchmaking(RoomType3, clientInfo)
      val room = Await.result(this.matchmaker2.joinMatchmaking(RoomType3, clientInfo), DefaultTimeout)
      assert(room.isInstanceOf[JoinedRoom])
    }
  }

}


@SerialVersionUID(12345L)
private[this] case class ClientInfo(a: Int) extends java.io.Serializable