package common.communication

import java.text.ParseException

import akka.http.scaladsl.model.ws.{Message, TextMessage}
import common.communication.CommunicationProtocol._

import scala.util.{Failure, Success, Try}

/**
 * A simple object that can write and read room protocol messages.
 * <br>
 * It handles them as text strings in the form<em>action{separator}payload</em>
 *
 * Internally assign unique string codes to [[common.communication.CommunicationProtocol.ClientMessageType]]
 */
object RoomProtocolSerializer extends SocketSerializer[RoomProtocolMessage] {
  val COMMAND_SEPARATOR = ":"
  val codeToString: Map[ProtocolMessageType, String] =
    Map(JoinRoom -> "0", LeaveRoom -> "1", MessageRoom -> "2") ++ //Client
      Map(JoinOk -> "3", Broadcast -> "4", Tell -> "5") //Room
  val stringToCode: Map[String, ProtocolMessageType] = codeToString.map(k => (k._2, k._1))


  override def parseFromSocket(msg: Message): Try[RoomProtocolMessage] = msg match {
    case TextMessage.Strict(message) => parseMessage(message)
    case msg => Failure(new ParseException(msg.toString, -1))
  }

  override def writeToSocket(msg: RoomProtocolMessage): Message = {
    TextMessage.Strict(codeToString(msg.messageType) + COMMAND_SEPARATOR + msg.payload)
  }

  private def parseMessage(msg: String): Try[RoomProtocolMessage] = {
    try {
      msg.split(COMMAND_SEPARATOR).toList match {
        case List(code) => Success(RoomProtocolMessage(this.stringToCode(code)))
        case List(code, payload) => Success(RoomProtocolMessage(this.stringToCode(code), payload))
      }
    } catch {
      case _: Exception => Failure(new ParseException(msg.toString, -1))
    }

  }
}
