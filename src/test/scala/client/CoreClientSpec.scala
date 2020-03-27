package client

import java.util.concurrent.TimeUnit

import akka.pattern.ask
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import client.MessageDictionary._
import common.Room
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

class CoreClientSpec extends TestKit(ActorSystem("ClientSystem", ConfigFactory.load()))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  private val serverUri = "http://localhost:8080"

  implicit val executionContext: ExecutionContext = system.dispatcher
  private val requestTimeout = 5 // Seconds
  import akka.util.Timeout
  implicit val timeout: Timeout = Timeout(requestTimeout, TimeUnit.SECONDS)

  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

  "Regarding joined rooms, a core client" must {
    val testActor = system actorOf CoreClient(serverUri)
    val A = "A"; val B = "B"
    val roomA = Room(A); val roomB = Room(B)

    "start with no joined rooms" in {
      (testActor ? GetJoinedRooms).onComplete(reply => {
        expectMsgClass(classOf[JoinedRooms])
        assert(reply.asInstanceOf[JoinedRooms].rooms.isEmpty)
      })
    }

    "add a room to joined room if such room was not previously joined" in {
      testActor ! NewJoinedRoom(roomA)
      (testActor ? GetJoinedRooms).onComplete(reply => {
        expectMsgClass(classOf[JoinedRooms])
        reply.asInstanceOf[JoinedRooms].rooms shouldEqual Set(roomA)
      })

      testActor ! NewJoinedRoom(roomB)
      (testActor ? GetJoinedRooms).onComplete(reply => {
        expectMsgClass(classOf[JoinedRooms])
        reply.asInstanceOf[JoinedRooms].rooms shouldEqual Set(roomA, roomB)
      })
    }

    "not add an already joined room" in {
      testActor ! NewJoinedRoom(roomA)
      (testActor ? GetJoinedRooms).onComplete(reply => {
        expectMsgClass(classOf[JoinedRooms])
        reply.asInstanceOf[JoinedRooms].rooms shouldEqual Set(roomA, roomB)
      })
    }
  }

  "When the client creates a new public room, the core client" must {

    val testActor = system actorOf CoreClient(serverUri)

    "add the new room to the set of joined rooms" in {
      testActor ! CreatePublicRoom
      Thread sleep 1000
      (testActor ? GetJoinedRooms).onComplete(reply => {
        expectMsgClass(classOf[JoinedRooms])
        reply.asInstanceOf[JoinedRooms].rooms.size shouldEqual 1
      })
    }
  }
}


