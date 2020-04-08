package common.communication

import common.communication.CommunicationProtocol.ProtocolMessageType.ProtocolMessageType

object CommunicationProtocol {
  object ProtocolMessageType extends Enumeration {
    type ProtocolMessageType = Value
    /**
     * Type of messages that clients can send to rooms
     */
    val JoinRoom: ProtocolMessageType = Value(0)
    val LeaveRoom: ProtocolMessageType = Value(1)
    val MessageRoom: ProtocolMessageType = Value(2)
    /**
     * Type of messages that rooms can send to clients
     */
    val JoinOk: ProtocolMessageType = Value(3)
    val ClientNotAuthorized: ProtocolMessageType = Value(4) // scalastyle:ignore magic.number
    val Broadcast: ProtocolMessageType = Value(5) // scalastyle:ignore magic.number
    val Tell: ProtocolMessageType = Value(6) // scalastyle:ignore magic.number
    val StateUpdate: ProtocolMessageType = Value(7) // scalastyle:ignore magic.number
  }

  /**
   * The message that clients and rooms will send through the socket
   *
   * @param messageType the type of message to send
   * @param payload     an optional payload
   */
  //TODO: generify this functions so that we can also pass different type of payloads other than strings
  @SerialVersionUID(1234L) // scalastyle:ignore magic.number
  case class RoomProtocolMessage(messageType: ProtocolMessageType, sessionId: String = "", payload: java.io.Serializable = "")
    extends java.io.Serializable
}

