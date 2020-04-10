package common.communication

import java.text.ParseException

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{BinaryMessage, Message}
import akka.stream.scaladsl.Sink
import akka.util.{ByteString, ByteStringBuilder}
import common.communication.CommunicationProtocol.RoomProtocolMessage
import org.apache.commons.lang3.SerializationUtils

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

/**
 * A SocketSerializer for [[common.communication.CommunicationProtocol.RoomProtocolMessage]] that can write and read
 * them as binary objects. The payload is serialized according to the java.io.Serialization methods
 */
object BinaryProtocolSerializer extends SocketSerializer[RoomProtocolMessage] {

  implicit val actorSystem = ActorSystem("remove")

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
