package server.room

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import common.communication.CommunicationProtocol.RoomProtocolMessage
import common.communication.RoomProtocolSerializer
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import server.room.socket.RoomSocketFlow
import common.communication.CommunicationProtocol.ProtocolMessageType._
import scala.concurrent.duration._
import scala.concurrent.Await

class RoomSocketFlowSpec extends TestKit(ActorSystem("RoomSocketFlow", ConfigFactory.load()))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll {


  private val MAX_AWAIT_SOCKET_MESSAGES = 10 seconds

  private var room = ServerRoom(UUID.randomUUID.toString)
  private var roomActor = system actorOf RoomActor(room)
  private var roomSocketFlow = RoomSocketFlow(roomActor, RoomProtocolSerializer)
  private var flow = roomSocketFlow.createFlow()

  override def afterAll(): Unit = {
    system.terminate()
  }

  before {
    room = ServerRoom(UUID.randomUUID.toString)
    roomActor = system actorOf RoomActor(room)
    roomSocketFlow = RoomSocketFlow(roomActor, RoomProtocolSerializer)
    flow = roomSocketFlow.createFlow()
  }

  "A RoomSocket" should {
    "forward room protocol messages from clients to room actors" in {
      val joinMessage = Source.single(RoomProtocolSerializer.prepareToSocket(RoomProtocolMessage(JoinRoom)))
      flow.runWith(joinMessage, Sink.ignore)
      flow.watchTermination()((_, f) => {
        Await.result(f, MAX_AWAIT_SOCKET_MESSAGES)
        assert(room.connectedClients.size == 1)
      })
    }

    "make sure that messages from the same socket are linked to the same client" in {
      val joinAndLeave = Source.fromIterator(() => Seq(
        RoomProtocolSerializer.prepareToSocket(RoomProtocolMessage(JoinRoom)),
        RoomProtocolSerializer.prepareToSocket(RoomProtocolMessage(LeaveRoom))
      ).iterator)
      flow.runWith(joinAndLeave, Sink.ignore)
      flow.watchTermination()((_, f) => {
        Await.result(f, MAX_AWAIT_SOCKET_MESSAGES)
        assert(room.connectedClients.isEmpty)
      })
    }

    "make sure that messages from different sockets are linked to different clients" in {
      val join = Source.single(RoomProtocolSerializer.prepareToSocket(RoomProtocolMessage(JoinRoom)))
      flow.runWith(join, Sink.ignore)
      flow.watchTermination()((_, f) => {
        Await.result(f, MAX_AWAIT_SOCKET_MESSAGES)
        assert(room.connectedClients.size == 1)
      })

      //Create a different client
      val leave = Source.single(RoomProtocolSerializer.prepareToSocket(RoomProtocolMessage(JoinRoom)))
      flow.runWith(leave, Sink.ignore)
      flow.watchTermination()((_, f) => {
        Await.result(f, MAX_AWAIT_SOCKET_MESSAGES)
        assert(room.connectedClients.size == 1)
      })
    }

  }


}
