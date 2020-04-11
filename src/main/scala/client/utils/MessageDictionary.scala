package client.utils

import akka.actor.ActorRef
import client.room.ClientRoom
import common.communication.CommunicationProtocol.RoomProtocolMessage
import common.communication.SocketSerializer
import common.room.{FilterOptions, RoomProperty}
import common.room.Room.{SharedRoom, RoomId, RoomPassword, RoomType}

object MessageDictionary {

  //CoreClient

  trait CreateRoomMessage

  case class CreatePublicRoom(roomType: RoomType, roomOption: Set[RoomProperty]) extends CreateRoomMessage

  case class CreatePrivateRoom(roomType: RoomType, roomOption: Set[RoomProperty], password: RoomPassword) extends CreateRoomMessage

  case class GetAvailableRooms(roomType: RoomType, roomOption: FilterOptions)

  case class GetJoinedRooms()

  case class JoinedRooms(joinedRooms: Set[ClientRoom])

  case class ClientRoomActorLeaved(clientRoomActor: ActorRef)

  case class ClientRoomActorJoined(clientRoomActor: ActorRef)

  case class HttpRoomResponse(room: SharedRoom)

  case class HttpRoomSequenceResponse(rooms: Seq[SharedRoom])

  case class FailResponse(ex: Throwable)

  case class RetrieveClientRoom()


  //HttpClient

  /**
   * Create a room and respond with [[HttpRoomResponse]] on success or [[FailResponse]] on failure
   */
  case class HttpPostRoom(roomType: RoomType, roomOption: Set[RoomProperty])


  /**
   * Get rooms and respond with [[HttpRoomSequenceResponse]] or [[FailResponse]]  on failure
   */
  case class HttpGetRooms(roomType: RoomType, roomOption: FilterOptions)

  /**
   * Perform a web socket request to open a connection to server side room with the given id.
   * If the connection is successful respond with message [[HttpSocketSuccess]] otherwise [[HttpSocketFail]]
   *
   * @param roomId id of the room to connect to
   * @param parser messages received on the socket will be parsed with this parser before sending them to the
   *               receiver actor
   */
  case class HttpSocketRequest[T](roomId: RoomId, parser: SocketSerializer[T])

  /**
   * Successful response of an [[HttpSocketRequest]].
   * Contains an actor ref.
   *
   * @param outRef Sending messages to this actor means sending them in the socket
   */
  case class HttpSocketSuccess(outRef: ActorRef)

  /**
   * Failure response of an [[HttpSocketRequest]].
   *
   * @param cause what caused the failure
   */
  case class HttpSocketFail(cause: String)


  //ClientRoomActor

  case class ClientRoomResponse(clientRoom: ClientRoom)

  case class SendJoin(roomId: RoomId, password: RoomPassword)

  case class SendLeave()

  case class SendProtocolMessage(msg: RoomProtocolMessage)

  case class SendStrictMessage(msg: Any with java.io.Serializable)

  /**
   * Define a callback that will be execute by the actor after a message received from the socket
   *
   * @param callback the callback that handles the message
   */
  case class OnMsgCallback(callback: Any => Unit)

  /**
   * Define a callback that will be execute by the actor after a message
   * that represent a new game state
   * @param callback the callback that handles the message
   */
  case class OnStateChangedCallback(callback:  Any with java.io.Serializable => Unit)
}
