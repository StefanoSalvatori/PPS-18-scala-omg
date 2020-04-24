package server.communication

import akka.actor.ActorRef
import common.communication.CommunicationProtocol.ProtocolMessageType._
import common.communication.CommunicationProtocol.{ProtocolMessage, ProtocolMessageSerializer, SessionId}
import common.room.Room.RoomPassword
import server.room.Client
import server.room.RoomActor.{Join, Leave, Msg, Reconnect}

/**
 * Define a socket flow that parse incoming messages as RoomProtocolMessages and convert them to messages that are
 * forwarded to a room actor.
 * It parses the messages with the parser specified in the constructor
 *
 * @param room             the room actor that will receive the messages
 * @param parser           the parsers to use for socket messages
 * @param connectionConfig socket connection configurations, see [[server.communication.ConnectionConfigurations]]
 */
case class RoomSocket(private val room: ActorRef,
                      override val parser: ProtocolMessageSerializer,
                      override val connectionConfig: ConnectionConfigurations = ConnectionConfigurations.Default)
  extends Socket[ProtocolMessage] {

  override protected val pingMessage: ProtocolMessage = ProtocolMessage(Ping)
  override protected val pongMessage: ProtocolMessage = ProtocolMessage(Pong)

  override protected val onMessageFromSocket: PartialFunction[ProtocolMessage, Unit] = {
    case ProtocolMessage(JoinRoom, SessionId.empty, payload) =>
      room ! Join(this.client, SessionId.empty, payload.asInstanceOf[RoomPassword])
    case ProtocolMessage(JoinRoom, sessionId, payload) =>
      this.client = Client.asActor(sessionId, this.clientActor)
      room ! Join(this.client, sessionId, payload.asInstanceOf[RoomPassword])
    case ProtocolMessage(ReconnectRoom, sessionId, payload) =>
      this.client = Client.asActor(sessionId, this.clientActor)
      room ! Reconnect(this.client, sessionId, payload.asInstanceOf[RoomPassword])
    case ProtocolMessage(LeaveRoom, _, _) =>
      room ! Leave(this.client)
    case ProtocolMessage(MessageRoom, _, payload) =>
      room ! Msg(this.client, payload)
  }

  override protected def onSocketClosed(): Unit = room ! Leave(client)

}
