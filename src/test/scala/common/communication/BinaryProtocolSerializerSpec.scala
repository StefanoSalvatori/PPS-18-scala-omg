package common.communication

import java.text.ParseException
import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{BinaryMessage, TextMessage}
import akka.util.ByteString
import common.communication.CommunicationProtocol.ProtocolMessageType.{JoinOk, LeaveRoom}
import common.communication.CommunicationProtocol.RoomProtocolMessage
import org.apache.commons.lang3.SerializationUtils
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec

import scala.concurrent.ExecutionContext

class BinaryProtocolSerializerSpec extends AnyFlatSpec with BeforeAndAfterAll {

  private implicit val actorSystem: ActorSystem = ActorSystem()
  private val serializer = BinaryProtocolSerializer()

  override def afterAll(): Unit = {
    super.afterAll()
    this.actorSystem.terminate()
  }

  behavior of "Room Protocol Binary Serializer"
/*
  it should "serialize and deserialize room protocol messages " in {
    val testMessage = RoomProtocolMessage(JoinOk, "", "random payload")
    val serialized = serializer.prepareToSocket(testMessage)
    assert(serializer.parseFromSocket(serialized).get equals testMessage)
  }


  it should "correctly parse text messages with no payload and no sessionId received from a socket" in {
    val testMessage = RoomProtocolMessage(JoinOk)
    val messageToReceive = BinaryMessage.Strict(ByteString(SerializationUtils.serialize(testMessage)))
    assert(serializer.parseFromSocket(messageToReceive).get == testMessage)

  }

  it should "correctly parse text messages with no payload received from a socket" in {
    val testMessage = RoomProtocolMessage(LeaveRoom, UUID.randomUUID.toString)
    val messageToReceive = BinaryMessage.Strict(ByteString(SerializationUtils.serialize(testMessage)))
    assert(serializer.parseFromSocket(messageToReceive).get == testMessage)
  }

  it should "fail to parse malformed messages" in {
    val messageToReceive = TextMessage.Strict("foo")
    val parseResult = serializer.parseFromSocket(messageToReceive)
    assert(parseResult.isFailure)
    assertThrows[ParseException] {
      parseResult.get
    }
  }*/

}
