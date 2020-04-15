package server.room

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import common.communication.CommunicationProtocol.RoomProtocolMessage
import common.communication.TextProtocolSerializer
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import server.room.socket.RoomSocket
import common.communication.CommunicationProtocol.ProtocolMessageType._
import org.scalatest.concurrent.Eventually
import server.RoomHandler

import scala.concurrent.duration._
import scala.concurrent.Await

class RoomSocketSpec extends TestKit(ActorSystem("RoomSocketFlow", ConfigFactory.load()))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with Eventually
  with BeforeAndAfter
  with BeforeAndAfterAll {


  private val MaxAwaitSocketMessages = 10 seconds

  private var room: ServerRoom = _
  private var roomActor: ActorRef = _
  private var roomSocketFlow: RoomSocket = _
  private var flow: Flow[Message, Message, Any] = _

  override def afterAll(): Unit = {
    system.terminate()
  }

  before {
    room = ServerRoom()
    roomActor = system actorOf RoomActor(room, RoomHandler())
    roomSocketFlow = RoomSocket(roomActor, TextProtocolSerializer)
    flow = roomSocketFlow.createFlow()
  }

  "A RoomSocket" should {
    "forward room protocol messages from clients to room actors" in {
      val joinMessage = Source.single(TextProtocolSerializer.prepareToSocket(RoomProtocolMessage(JoinRoom)))
      flow.runWith(joinMessage, Sink.ignore)
      flow.watchTermination()((_, f) => {
        Await.result(f, MaxAwaitSocketMessages)
        assert(room.connectedClients.size == 1)
      })
    }

    "make sure that messages from the same socket are linked to the same client" in {
      val joinAndLeave = Source.fromIterator(() => Seq(
        TextProtocolSerializer.prepareToSocket(RoomProtocolMessage(JoinRoom)),
        TextProtocolSerializer.prepareToSocket(RoomProtocolMessage(LeaveRoom))
      ).iterator)
      flow.runWith(joinAndLeave, Sink.ignore)
      flow.watchTermination()((_, f) => {
        Await.result(f, MaxAwaitSocketMessages)
        assert(room.connectedClients.isEmpty)
      })
    }

    "make sure that messages from different sockets are linked to different clients" in {
      val join = Source.single(TextProtocolSerializer.prepareToSocket(RoomProtocolMessage(JoinRoom)))
      flow.runWith(join, Sink.ignore)
      flow.watchTermination()((_, f) => {
        Await.result(f, MaxAwaitSocketMessages)
        assert(room.connectedClients.size == 1)
      })

      //Create a different client
      val leave = Source.single(TextProtocolSerializer.prepareToSocket(RoomProtocolMessage(JoinRoom)))
      flow.runWith(leave, Sink.ignore)
      flow.watchTermination()((_, f) => {
        Await.result(f, MaxAwaitSocketMessages)
        assert(room.connectedClients.size == 1)
      })
    }

    "automatically remove clients when their socket is closed" in {
      val join = Source.single(TextProtocolSerializer.prepareToSocket(RoomProtocolMessage(JoinRoom)))
      flow.runWith(join, Sink.ignore)
      flow.watchTermination()((_, f) => {
        Await.result(f, MaxAwaitSocketMessages)
        assert(room.connectedClients.size == 1)
      })

      eventually {
        assert(room.connectedClients.isEmpty)
      }
    }
  }


}
