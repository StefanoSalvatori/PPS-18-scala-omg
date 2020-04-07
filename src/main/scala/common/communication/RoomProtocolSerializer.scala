package common.communication

import java.text.ParseException

import akka.http.scaladsl.model.ws.{Message, TextMessage}
import common.communication.CommunicationProtocol._

import scala.util.{Failure, Success, Try}

/**
 * A simple object that can write and read room protocol messages.
 * <br>
 * It handles them as text strings in the form <em>action{separator}sessionId{separator}payload</em>
 */
object RoomProtocolSerializer extends SocketSerializer[RoomProtocolMessage] {

  import ProtocolMessageType._
  val SEPARATOR = ":"
  private val ProtocolFieldsCount = 3


  override def parseFromSocket(msg: Message): Try[RoomProtocolMessage] = msg match {
    case TextMessage.Strict(message) => parseMessage(message)
    case msg => Failure(new ParseException(msg.toString, -1))
  }

  override def prepareToSocket(msg: RoomProtocolMessage): Message = {
    TextMessage.Strict(msg.messageType.id.toString + SEPARATOR + msg.sessionId + SEPARATOR + msg.payload)
  }

  private def parseMessage(msg: String): Try[RoomProtocolMessage] = {
    try {
      msg.split(SEPARATOR, ProtocolFieldsCount).toList match {
        case List(code) => Success(RoomProtocolMessage(ProtocolMessageType(code.toInt)))
        case List(code, sessionId) => Success(RoomProtocolMessage(ProtocolMessageType(code.toInt), sessionId))
        case List(code, sessionId, payload) => Success(RoomProtocolMessage(ProtocolMessageType(code.toInt), sessionId, payload))
      }
    } catch {
      case e: NoSuchElementException => Failure(e)
      case _: Exception => Failure(new ParseException(msg.toString, -1))
    }
  }


}
