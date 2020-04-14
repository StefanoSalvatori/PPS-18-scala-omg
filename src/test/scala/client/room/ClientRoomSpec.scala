package client.room

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.TestKit
import client.utils.MessageDictionary.{CreatePrivateRoom, CreatePublicRoom, GetJoinedRooms, JoinedRooms}
import client.CoreClient
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import common.TestConfig
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import server.GameServer
import akka.pattern.ask
import akka.util.Timeout
import common.http.Routes

import common.room.{Room, RoomProperty}
import server.utils.ExampleRooms.{MyRoom, NoPropertyRoom}

import common.room.RoomJsonSupport
import server.utils.ExampleRooms
import server.utils.ExampleRooms.ClosableRoomWithState


import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Promise}
import scala.util.Try
import common.room.RoomPropertyValueConversions._

class ClientRoomSpec extends TestKit(ActorSystem("ClientSystem", ConfigFactory.load()))
  with TestConfig
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll
  with LazyLogging
  with RoomJsonSupport {

  private val ServerAddress = "localhost"
  private val ServerPort = ClientRoomSpecServerPort

  private val ServerLaunchAwaitTime = 10 seconds
  private val ServerShutdownAwaitTime = 10 seconds
  implicit private val DefaultTimeout: Timeout = 5 seconds
  implicit private val DefaultDuration: Duration = 5 seconds

  implicit val execContext: ExecutionContextExecutor = system.dispatcher
  private var gameServer: GameServer = _
  private var coreClient: ActorRef = _
  private var clientRoom: ClientRoom = _

  before {
    gameServer = GameServer(ServerAddress, ServerPort)
    gameServer.defineRoom(ExampleRooms.roomWithStateType, ClosableRoomWithState)
    gameServer.defineRoom(ExampleRooms.myRoomType, MyRoom)
    gameServer.defineRoom(ExampleRooms.noPropertyRoomType, NoPropertyRoom)
    Await.ready(gameServer.start(), ServerLaunchAwaitTime)
    logger debug s"Server started at $ServerAddress:$ServerPort"
    coreClient = system actorOf CoreClient(Routes.httpUri(ServerAddress, ServerPort))
    val res = Await.result((coreClient ? CreatePublicRoom(ExampleRooms.roomWithStateType, Set.empty))
      .mapTo[Try[ClientRoom]], DefaultDuration)
    clientRoom = res.get
  }

  after {
    Await.ready(gameServer.terminate(), ServerShutdownAwaitTime)
  }

  "A client room" must {
    "join and notify the core client" in {
      Await.result(clientRoom.join(), DefaultDuration)
      val res = Await.result((coreClient ? GetJoinedRooms).mapTo[JoinedRooms], DefaultDuration).joinedRooms
      res should have size 1
    }

    "leave and notify the core client" in {
      Await.result(clientRoom.join(), DefaultDuration)
      Await.result(clientRoom.leave(), DefaultDuration)

      val res = Await.result((coreClient ? GetJoinedRooms).mapTo[JoinedRooms], DefaultDuration).joinedRooms
      res should have size 0
    }

    "show no property if no property is defined in the room (except for the private flag)" in {
      val res = Await.result((coreClient ? CreatePublicRoom(ExampleRooms.noPropertyRoomType, Set.empty)).mapTo[Try[ClientRoom]], DefaultDuration)
      val room = res.get
      room.properties should have size 1 // just private flag
    }

    "show correct default room properties when those are not overridden" in {
      val res = Await.result((coreClient ? CreatePublicRoom(ExampleRooms.myRoomType, Set.empty)).mapTo[Try[ClientRoom]], DefaultDuration)
      val room = res.get
      room.properties should have size 3 // a, b, private
      room.properties should contain("a", 0)
      room.properties should contain("b", "abc")
    }

    "show correct room properties when default values are overridden" in {
      val properties = Set(RoomProperty("a", 1), RoomProperty("b", "qwe"))
      val res = Await.result((coreClient ? CreatePublicRoom(ExampleRooms.myRoomType, properties)).mapTo[Try[ClientRoom]], DefaultDuration)
      val room = res.get
      room propertyOf "a" shouldEqual RoomProperty("a", 1)
      room propertyOf "b" shouldEqual RoomProperty("b", "qwe")
    }

    "show correct property values when those are not overridden" in {
      val res = Await.result((coreClient ? CreatePublicRoom(ExampleRooms.myRoomType, Set.empty)).mapTo[Try[ClientRoom]], DefaultDuration)
      val room = res.get
      room valueOf "a" shouldEqual 0
      room valueOf "b" shouldEqual "abc"
    }

    "show correct property values when those are overridden" in {
      val properties = Set(RoomProperty("a", 1), RoomProperty("b", "qwe"))
      val res = Await.result((coreClient ? CreatePublicRoom(ExampleRooms.myRoomType, properties)).mapTo[Try[ClientRoom]], DefaultDuration)
      val room = res.get
      room.properties should have size 3 // a, b, private
      room.properties should contain("a", 1)
      room.properties should contain("b", "qwe")
      room.properties should contain(Room.roomPrivateStatePropertyName, false)
    }

    "have the private flag turned on when a private room is created" in {
      val res = Await.result((coreClient ? CreatePrivateRoom(ExampleRooms.myRoomType, Set.empty, "pwd")).mapTo[Try[ClientRoom]], DefaultDuration)
      val room = res.get
      room valueOf Room.roomPrivateStatePropertyName shouldEqual true
    }

    "have the private flag turned off when a public room is created" in {
      val res = Await.result((coreClient ? CreatePublicRoom(ExampleRooms.myRoomType, Set.empty)).mapTo[Try[ClientRoom]], DefaultDuration)
      val room = res.get
      room valueOf Room.roomPrivateStatePropertyName shouldEqual false
    }

    "define a callback to handle messages from server room" in {
      val p = Promise[String]()
      Await.ready(clientRoom.join(), DefaultDuration)

      clientRoom.onMessageReceived { m =>
        p.success(m.toString)
      }
      clientRoom.send("ping")

      val res = Await.result(p.future, DefaultDuration)
      res shouldEqual "pong"
    }

    "define a callback to handle state changed" in {
      val p = Promise[Boolean]()

      clientRoom.onStateChanged { _ =>
        p.success(true)
      }
      Await.ready(clientRoom.join(), DefaultDuration)

      val res = Await.result(p.future, DefaultDuration)
      res shouldBe true
    }

    "define a callback to handle room closed changed" in {
      val p = Promise[Boolean]()

      Await.ready(clientRoom.join(), DefaultDuration)
      clientRoom.onClose {
        p.success(true)
      }
      clientRoom.send("close")
      val res = Await.result(p.future, DefaultDuration)
      res shouldBe true
    }
  }
}
