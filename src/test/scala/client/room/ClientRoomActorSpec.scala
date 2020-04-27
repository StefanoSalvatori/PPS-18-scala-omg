package client.room

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import client.CoreClient
import client.utils.MessageDictionary._
import com.typesafe.config.ConfigFactory
import common.communication.CommunicationProtocol.ProtocolMessageType._
import common.communication.CommunicationProtocol.{ProtocolMessage, ProtocolMessageType}
import common.http.Routes
import common.room.Room
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import server.GameServer
import server.room.ServerRoom
import test_utils.{ExampleRooms, TestConfig}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.{Success, Try}

class ClientRoomActorSpec extends TestKit(ActorSystem("ClientSystem", ConfigFactory.load()))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll
  with TestConfig {

  implicit val executionContext: ExecutionContext = system.dispatcher
  private val serverAddress = "localhost"
  private val serverPort = ClientRoomActorSpecServerPort
  private val serverUri = Routes.httpUri(serverAddress, serverPort)
  private var coreClient: ActorRef = _
  private var clientRoomActor: ActorRef = _
  private var gameServer: GameServer = _

  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

  before {
    coreClient = system actorOf CoreClient(serverUri)
    gameServer = GameServer(serverAddress, serverPort)
    gameServer.defineRoom(ExampleRooms.closableRoomWithStateType, () => ExampleRooms.ClosableRoomWithState())
    Await.ready(gameServer.start(), DefaultDuration)

    coreClient ! CreatePublicRoom(ExampleRooms.closableRoomWithStateType, Set.empty)
    val room = expectMsgType[Success[ClientRoom]]
    clientRoomActor = system actorOf ClientRoomActor(coreClient, serverUri, room.value)
  }

  after {
    Await.ready(gameServer.terminate(), ServerShutdownAwaitTime)
  }

  "Client Room Actor" should {
    "should respond with a success or a failure when joining" in {
      clientRoomActor ! SendJoin(None, Room.DefaultPublicPassword)
      expectMsgType[Try[Any]]
    }


    "should respond with a success or a failure when leaving" in {
      clientRoomActor ! SendJoin(None, Room.DefaultPublicPassword)
      expectMsgType[Try[Any]]

      clientRoomActor ! SendLeave
      expectMsgType[Try[Any]]
    }


    "should handle messages when receiving 'Tell' from server room" in {
      assert(this.checkCallback(Tell))
    }

    "should handle messages when receiving 'Broadcast' from server room" in {
      assert(this.checkCallback(Broadcast))
    }

    "should handle messages when receiving 'StateUpdate' from server room" in {
      assert(this.checkCallback(StateUpdate))
    }

    "should handle messages when receiving 'RoomClosed' from server room" in {
      assert(this.checkCallback(RoomClosed))
    }

    "should react to socket errors" in {
      val promise = Promise[Boolean]()
      clientRoomActor ! SendJoin(None, Room.DefaultPublicPassword)
      expectMsgType[Success[_]]

      clientRoomActor ! OnErrorCallback(_ => promise.success(true))
      clientRoomActor ! SocketError(new Exception("error"))

      assert(Await.result(promise.future, DefaultDuration))
    }

    "should handle callbacks defined before joining" in {
      val onMsgPromise = Promise[Boolean]()
      val onStateChangePromise = Promise[Boolean]()
      val onClosePromise = Promise[Boolean]()

      clientRoomActor ! OnMsgCallback(_ => onMsgPromise.success(true))
      clientRoomActor ! OnStateChangedCallback(_ =>
        if (!onStateChangePromise.isCompleted) {
          onStateChangePromise.success(true)
        }
      )
      clientRoomActor ! OnCloseCallback(() => onClosePromise.success(true))
      clientRoomActor ! SendJoin(None, Room.DefaultPublicPassword)
      expectMsgType[Success[_]]

      clientRoomActor ! SendStrictMessage("ping")
      assert(Await.result(onMsgPromise.future, DefaultDuration))

      clientRoomActor ! SendStrictMessage("changeState")
      assert(Await.result(onStateChangePromise.future, DefaultDuration))

      clientRoomActor ! SendStrictMessage("close")
      assert(Await.result(onClosePromise.future, DefaultDuration))

    }

    "should receive all messages from the server room" in {
      val messagesCount = 200
      val allReceived = Promise[Int]()
      var count = 0
      clientRoomActor ! SendJoin(None, Room.DefaultPublicPassword)
      expectMsgType[Success[_]]
      clientRoomActor ! OnMsgCallback(_ => {
        count = count + 1
        if(count == messagesCount) allReceived.success(count)
      })

      (0 until messagesCount).foreach(_ => {
        clientRoomActor ! SendStrictMessage("ping")
        Thread.sleep(10)
      })
      val received = Await.result(allReceived.future, 10 seconds)

      assert(received == messagesCount)

    }
  }


  private def checkCallback(msgType: ProtocolMessageType): Boolean = {
    val promise = Promise[Boolean]()
    msgType match {
      case Broadcast | Tell => clientRoomActor ! OnMsgCallback(_ => promise.success(true))
      case StateUpdate => clientRoomActor ! OnStateChangedCallback(_ => promise.success(true))
      case RoomClosed => clientRoomActor ! OnCloseCallback(() => promise.success(true))
    }

    clientRoomActor ! SendJoin(None, Room.DefaultPublicPassword)
    expectMsgType[Try[Any]]

    clientRoomActor ! ProtocolMessage(msgType)
    Await.result(promise.future, DefaultDuration)
  }

}

