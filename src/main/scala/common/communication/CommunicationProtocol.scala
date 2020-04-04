package common.communication

object CommunicationProtocol {

  sealed trait ProtocolMessageType
  /**
   * Type of messages that clients can send to rooms
   */
  sealed trait ClientMessageType extends ProtocolMessageType
  case object JoinRoom extends ClientMessageType
  case object LeaveRoom extends ClientMessageType
  case object MessageRoom extends ClientMessageType


  /**
   * Type of messages that rooms can send to clients
   */
  sealed trait RoomMessageType extends ProtocolMessageType
  case object JoinOk extends RoomMessageType
  case object Broadcast extends RoomMessageType
  case object Tell extends RoomMessageType



  /**
   * The message that clients and rooms will send through the socket
   *
   * @param messageType  the type of message to send
   * @param payload an optional payload
   */
  //TODO: generify this functions so that we can also pass different type of payloads other than strings
  case class RoomProtocolMessage(messageType: ProtocolMessageType, payload: String = "")

}
