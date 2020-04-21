package server.communication

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{Concat, Flow, Sink, Source}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import common.communication.CommunicationProtocol.ProtocolMessage
import common.communication.CommunicationProtocol.ProtocolMessageType._
import common.communication.TextProtocolSerializer
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import server.room.{RoomActor, ServerRoom}
import server.{RoomHandler, communication}

import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}

class RoomSocketSpec extends TestKit(ActorSystem("RoomSocketFlow", ConfigFactory.load()))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with Eventually
  with BeforeAndAfter
  with BeforeAndAfterAll {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(20 seconds, 25 millis)

  private val MaxAwaitSocketMessages = 2 seconds
  private val IdleConnectionTimeout = 2 seconds
  private val KeepAliveRate = 300 millis


  private var room: ServerRoom = _
  private var roomActor: ActorRef = _
  private var roomSocketFlow: RoomSocket = _
  private var flow: Flow[Message, Message, Any] = _
  private var flowTerminated: Promise[Boolean] = _


  override def afterAll(): Unit = {
    system.terminate()
  }

  before {
    room = ServerRoom()
    roomActor = system actorOf RoomActor(room, RoomHandler())
    roomSocketFlow = communication.RoomSocket(roomActor, TextProtocolSerializer, ConnectionConfigurations(IdleConnectionTimeout))
    flow = roomSocketFlow.createFlow()
    flowTerminated = Promise[Boolean]()
  }

  "A RoomSocket" should {
    "forward room protocol messages from clients to room actors" in {
      val client = this.joinAndIdle(10 seconds)
      flow.runWith(client, Sink.onComplete(_ => flowTerminated.success(true)))
      eventually {
        assert(room.connectedClients.size == 1)
      }
    }

    "make sure that messages from the same socket are linked to the same client" in {
      val joinAndLeave = Source.fromIterator(() => Seq(
        TextProtocolSerializer.prepareToSocket(ProtocolMessage(JoinRoom)),
        TextProtocolSerializer.prepareToSocket(ProtocolMessage(LeaveRoom))
      ).iterator)
      flow.runWith(joinAndLeave, Sink.onComplete(_ => flowTerminated.success(true)))
      Await.result(flowTerminated.future, MaxAwaitSocketMessages)
      eventually {
        assert(room.connectedClients.isEmpty)
      }
    }

    "make sure that messages from different sockets are linked to different clients" in {
      val client = this.joinAndIdle()
      flow.runWith(client, Sink.onComplete(_ => flowTerminated.success(true)))
      eventually {
        assert(room.connectedClients.size == 1)
      }
      //Create a different client
      val client2Socket = communication.RoomSocket(roomActor, TextProtocolSerializer)
      val client2Flow = client2Socket.createFlow()
      val flow2Terminated = Promise[Boolean]()
      val client2 = Source.single(TextProtocolSerializer.prepareToSocket(ProtocolMessage(LeaveRoom)))
      client2Flow.runWith(client2, Sink.onComplete(_ => flow2Terminated.success(true)))
      Await.result(flow2Terminated.future, MaxAwaitSocketMessages)
      assert(room.connectedClients.size == 1)
    }

    "automatically remove clients when their socket is closed" in {
      val client = joinAndIdle(100 millis)
      flow.runWith(client, Sink.onComplete(_ => flowTerminated.success(true)))
      Await.ready(flowTerminated.future, MaxAwaitSocketMessages)
      eventually {
        assert(room.connectedClients.isEmpty)
      }
    }

    "automatically close the socket after the idle-connection-timeout" in {
      val client = this.joinAndIdle()
      flow.runWith(client, Sink.onComplete(_ => flowTerminated.success(true)))
      Await.result(flowTerminated.future, MaxAwaitSocketMessages)
      eventually {
        assert(room.connectedClients.isEmpty)
      }
    }

    "close the socket if heartbeat is configured and client doesn't respond" in {
      roomSocketFlow = communication.RoomSocket(roomActor, TextProtocolSerializer, ConnectionConfigurations(keepAlive = KeepAliveRate))
      flow = roomSocketFlow.createFlow()
      flowTerminated = Promise[Boolean]()
      val client = this.joinAndIdle()
      flow.runWith(client, Sink.onComplete(_ => flowTerminated.success(true)))
      Await.ready(flowTerminated.future, MaxAwaitSocketMessages)
    }
  }

  def joinAndIdle(idleTime: FiniteDuration = 10000 seconds): Source[Message, NotUsed] = Source.combine(
    Source.single(TextProtocolSerializer.prepareToSocket(ProtocolMessage(JoinRoom))),
    Source.single(TextMessage("")).delay(idleTime))(Concat(_))

  def heartbeat(heartBeatRate: FiniteDuration): Source[Message, NotUsed] =
    Source.repeat(TextProtocolSerializer.prepareToSocket(ProtocolMessage(Pong)))
      .throttle(1, heartBeatRate)
}
