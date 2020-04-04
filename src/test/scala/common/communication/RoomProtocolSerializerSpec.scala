package common.communication

import akka.http.scaladsl.model.ws.TextMessage
import common.communication.CommunicationProtocol._
import org.scalatest.flatspec.AnyFlatSpec

class RoomProtocolSerializerSpec extends AnyFlatSpec {


  behavior of "Room Protocol Serializer"

  it should "map action codes to unique strings" in {
    val uniqueValues = RoomProtocolSerializer.codeToString.values.toSet
    assert(uniqueValues.size == RoomProtocolSerializer.codeToString.keys.size)
  }

  it should s"write messages to sockets in the format 'action${RoomProtocolSerializer.COMMAND_SEPARATOR}payload'" in {
    val messageToSend = RoomProtocolMessage(MessageRoom, "Hello")
    val written = RoomProtocolSerializer.writeToSocket(messageToSend)
    val expected =
      TextMessage.Strict(RoomProtocolSerializer.codeToString(MessageRoom) + RoomProtocolSerializer.COMMAND_SEPARATOR + "Hello")
    assert(written == expected)

  }

  it should "parse text messages with no payload received from a socket" in {
    val messageToReceive = TextMessage.Strict(s"0${RoomProtocolSerializer.COMMAND_SEPARATOR}")
    val expected = RoomProtocolMessage(RoomProtocolSerializer.stringToCode("0"))
    assert(RoomProtocolSerializer.parseFromSocket(messageToReceive).get == expected)

  }

  it should "parse text messages with payload received from a socket" in {
    val messageToReceive = TextMessage.Strict(s"1${RoomProtocolSerializer.COMMAND_SEPARATOR}Payload")
    val expected = RoomProtocolMessage(RoomProtocolSerializer.stringToCode("1"), "Payload")
    assert(RoomProtocolSerializer.parseFromSocket(messageToReceive).get == expected)

  }

  it should "fail to parse malformed messages" in {
    val messageToReceive = TextMessage.Strict("foo")
    assert(RoomProtocolSerializer.parseFromSocket(messageToReceive).isFailure)
  }

  it should "fail to parse messages with an unknown type" in {
    val messageToReceive = TextMessage.Strict(s"97${RoomProtocolSerializer.COMMAND_SEPARATOR}Payload")
    assert(RoomProtocolSerializer.parseFromSocket(messageToReceive).isFailure)
  }

}
