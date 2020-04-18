package common.communication

import java.text.ParseException

import akka.http.scaladsl.model.ws.{Message, TextMessage}
import common.communication.CommunicationProtocol._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
 * A simple object that can write and read room protocol messages as text strings.
 * <br>
 * It handles them as text strings in the form <em>action{separator}sessionId{separator}payload</em>
 */
object TextProtocolSerializer extends RoomProtocolMessageSerializer {
  val SEPARATOR = ":"
  private val ProtocolFieldsCount = 3


  override def parseFromSocket(msg: Message): Future[RoomProtocolMessage] = msg match {
    case TextMessage.Strict(message) => parseMessage(message)
    case msg => Future.failed(new ParseException(msg.toString, -1))
  }

  override def prepareToSocket(msg: RoomProtocolMessage): Message = {
    TextMessage.Strict(msg.messageType.id.toString + SEPARATOR + msg.sessionId + SEPARATOR + msg.payload)
  }

  private def parseMessage(msg: String): Future[RoomProtocolMessage] = {
    try {
      msg.split(SEPARATOR, ProtocolFieldsCount).toList match {
        case List(code, sessionId, payload) =>
          Future.successful(RoomProtocolMessage(ProtocolMessageType(code.toInt), sessionId, payload))
      }
    } catch {
      case e: NoSuchElementException => Future.failed(e)
      case _: Exception => Future.failed(new ParseException(msg.toString, -1))
    }
  }


}
