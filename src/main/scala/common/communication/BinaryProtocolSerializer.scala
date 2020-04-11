package common.communication

import java.text.ParseException

import akka.http.scaladsl.model.ws.{BinaryMessage, Message}
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import common.communication.CommunicationProtocol.RoomProtocolMessage
import org.apache.commons.lang3.SerializationUtils

import scala.util.{Failure, Success, Try}

/**
 * A SocketSerializer for [[common.communication.CommunicationProtocol.RoomProtocolMessage]] that can write and read
 * them as binary objects. The payload is serialized according to the java.io.Serialization methods
 */
case class BinaryProtocolSerializer(implicit val materializer: Materializer) extends SocketSerializer[RoomProtocolMessage] {


  override def parseFromSocket(msg: Message): Try[RoomProtocolMessage] = msg match {
    case BinaryMessage.Strict(binaryMessage) => parseBinaryMessage(binaryMessage)

    // ignore binary messages but drain content to avoid the stream being clogged
    case bm@BinaryMessage.Streamed(_) =>
      parseIgnoreStream(bm)
      Failure(new ParseException("ignore stream", 0))
    case _ => Failure(new ParseException(msg.toString, -1))
  }

  override def prepareToSocket(msg: RoomProtocolMessage): BinaryMessage =
    BinaryMessage.Strict(ByteString(SerializationUtils.serialize(msg)))


  private def parseIgnoreStream(bm: BinaryMessage.Streamed): Unit = {
    bm.dataStream.runWith(Sink.ignore)
  }

  private def parseBinaryMessage(msg: ByteString): Try[RoomProtocolMessage] = {
    try {
      Success(SerializationUtils.deserialize(msg.toArray).asInstanceOf[RoomProtocolMessage])
    } catch {
      case _: Exception => Failure(new ParseException(msg.toString, -1))
    }
  }
}
