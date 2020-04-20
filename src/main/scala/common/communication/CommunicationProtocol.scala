package common.communication

import common.communication.CommunicationProtocol.ProtocolMessageType.ProtocolMessageType

object CommunicationProtocol {
  type SessionId = String
  object SessionId {
    val empty: SessionId = ""
  }

  /**
   * Anything that can be serialized and sent to the socket
   */
  type SocketSerializable = Any with java.io.Serializable


  object ProtocolMessageType extends Enumeration {
    type ProtocolMessageType = Value
    /**
     * Type of messages that clients can send to rooms
     */
    val JoinRoom: ProtocolMessageType = Value(0) // scalastyle:ignore magic.number
    val LeaveRoom: ProtocolMessageType = Value(1) // scalastyle:ignore magic.number
    val MessageRoom: ProtocolMessageType = Value(2) // scalastyle:ignore magic.number
    val CloseRoom: ProtocolMessageType = Value(3) // scalastyle:ignore magic.number
    val Pong: ProtocolMessageType = Value(4) // scalastyle:ignore magic.number

    /**
     * Type of messages that rooms can send to clients
     */
    val JoinOk: ProtocolMessageType = Value(5) // scalastyle:ignore magic.number
    val ClientNotAuthorized: ProtocolMessageType = Value(6) // scalastyle:ignore magic.number
    val Broadcast: ProtocolMessageType = Value(7) // scalastyle:ignore magic.number
    val Tell: ProtocolMessageType = Value(8) // scalastyle:ignore magic.number
    val StateUpdate: ProtocolMessageType = Value(9) // scalastyle:ignore magic.number
    val RoomClosed: ProtocolMessageType = Value(10) // scalastyle:ignore magic.number
    val LeaveOk: ProtocolMessageType = Value(11) // scalastyle:ignore magic.number
    val Ping: ProtocolMessageType = Value(12) // scalastyle:ignore magic.number
  }

  /**
   * The message that clients and rooms will send through the socket
   *
   * @param messageType the type of message to send
   * @param payload     an optional payload
   */
  @SerialVersionUID(1234L) // scalastyle:ignore magic.number
  case class RoomProtocolMessage(messageType: ProtocolMessageType, sessionId: SessionId = SessionId.empty, payload: java.io
  .Serializable = "")
    extends java.io.Serializable

  /**
   * A socket serializer for room protocol messages
   */
  trait RoomProtocolMessageSerializer extends SocketSerializer[RoomProtocolMessage]
}

