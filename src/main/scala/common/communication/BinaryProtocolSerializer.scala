package common.communication

import java.text.ParseException

import akka.http.scaladsl.model.ws.{BinaryMessage, Message}
import akka.util.ByteString
import common.communication.CommunicationProtocol.RoomProtocolMessage
import org.apache.commons.lang3.SerializationUtils

import scala.util.{Failure, Success, Try}


/**
 * A SocketSerializer for [[common.communication.CommunicationProtocol.RoomProtocolMessage]] that can write and read
 * them as binary objects. The payload is serialized according to the java.io.Serialization methods
 */
object BinaryProtocolSerializer extends SocketSerializer[RoomProtocolMessage] {

  override def parseFromSocket(msg: Message): Try[RoomProtocolMessage] = msg match {
    case BinaryMessage.Strict(binaryMessage) => parseBinaryMessage(binaryMessage)
    case _ => Failure(new ParseException(msg.toString, -1))
  }

  override def prepareToSocket(msg: RoomProtocolMessage): BinaryMessage =
    BinaryMessage.Strict(ByteString(SerializationUtils.serialize(msg)))


  private def parseBinaryMessage(msg: ByteString): Try[RoomProtocolMessage] = {
    try {
      Success(SerializationUtils.deserialize(msg.toArray).asInstanceOf[RoomProtocolMessage])
    } catch {
      case _: Exception => Failure(new ParseException(msg.toString, -1))
    }
  }
}
