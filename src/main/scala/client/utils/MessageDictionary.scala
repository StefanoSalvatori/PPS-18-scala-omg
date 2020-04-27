package client.utils

import akka.actor.ActorRef
import client.room.JoinedRoom
import common.communication.CommunicationProtocol.SocketSerializable
import common.communication.SocketSerializer
import common.room.Room.{RoomId, RoomPassword, RoomType}
import common.room.{FilterOptions, RoomProperty, SharedRoom}

// scalastyle:ignore method.length
private[client] object MessageDictionary {

  //CoreClient
  trait CreateRoomMessage
  case class CreatePublicRoom(roomType: RoomType, roomOption: Set[RoomProperty]) extends CreateRoomMessage
  case class CreatePrivateRoom(roomType: RoomType, roomOption: Set[RoomProperty], password: RoomPassword) extends CreateRoomMessage

  case class GetAvailableRooms(roomType: RoomType, roomOption: FilterOptions)

  case class GetJoinedRooms()

  case class JoinedRooms(joinedRooms: Set[JoinedRoom])

  case class ClientRoomActorLeft(clientRoomActor: ActorRef)

  case class ClientRoomActorJoined(clientRoomActor: ActorRef)

  case class HttpRoomResponse(room: SharedRoom)

  case class HttpRoomSequenceResponse(rooms: Seq[SharedRoom])

  case class FailResponse(ex: Throwable)

  case class RetrieveClientRoom()


  //HttpClient

  /**
   * Create a room and respond with [[client.utils.MessageDictionary.HttpRoomResponse]] on success or
   * [[client.utils.MessageDictionary.FailResponse]] on failure
   */
  case class HttpPostRoom(roomType: RoomType, roomOption: Set[RoomProperty])


  /**
   * Get rooms and respond with [[client.utils.MessageDictionary.HttpRoomSequenceResponse]]
   * or [[client.utils.MessageDictionary.FailResponse]]  on failure
   */
  case class HttpGetRooms(roomType: RoomType, roomOption: FilterOptions)

  /**
   * Perform a web socket request to open a connection to server side room with the given id.
   * If the connection is successful respond with message [[client.utils.MessageDictionary.HttpSocketSuccess]]
   * otherwise [[client.utils.MessageDictionary.HttpSocketFail]]
   *
   * @param roomId id of the room to connect to
   * @param parser messages received on the socket will be parsed with this parser before sending them to the
   *               receiver actor
   */
  case class HttpRoomSocketRequest[T](roomId: RoomId, parser: SocketSerializer[T])

  /**
   * Perform a web socket request to open a connection with the server side matchmaking service.
   * If the connection is successful respond with message [[client.utils.MessageDictionary.HttpSocketSuccess]]
   * otherwise [[client.utils.MessageDictionary.HttpSocketFail]]
   *
   * @param roomType id of the room to connect to
   * @param parser   messages received on the socket will be parsed with this parser before sending them to the
   *                 receiver actor
   */
  case class HttpMatchmakingSocketRequest[T](roomType: RoomType, parser: SocketSerializer[T])

  /**
   * Successful response of an [[client.utils.MessageDictionary.HttpRoomSocketRequest]].
   * Contains an actor ref.
   *
   * @param outRef Sending messages to this actor means sending them in the socket
   */
  case class HttpSocketSuccess(outRef: ActorRef)

  /**
   * Failure response of an [[HttpRoomSocketRequest]].
   *
   * @param cause what caused the failure
   */
  case class HttpSocketFail(cause: String)


  //ClientRoomActor

  case class ClientRoomResponse(clientRoom: JoinedRoom)

  case class SendJoin(sessionId: Option[String], password: RoomPassword)

  case class SendReconnect(sessionId: Option[String], password: RoomPassword)

  case class SendLeave()

  case class SendStrictMessage(msg: SocketSerializable)

  /**
   * Sent to the actor when an error occurs on the socket
   *
   * @param exception exception thrown
   */
  case class SocketError(exception: Throwable)

  /**
   * Define a callback that will be execute by the actor after an error occurs on the socket
   *
   * @param callback the callback that handles the error
   */
  case class OnErrorCallback(callback: Throwable => Unit)


  /**
   * Define a callback that will be execute by the actor after a message received from the socket
   *
   * @param callback the callback that handles the message
   */
  case class OnMsgCallback(callback: Any => Unit)

  /**
   * Define a callback that will be execute by the actor after a message
   * that represent a new game state
   *
   * @param callback the callback that handles the message
   */
  case class OnStateChangedCallback(callback: Any => Unit)

  /**
   * Define a callback that will be execute by the actor after the room has been closed
   *
   * @param callback the callback that handles the message
   */
  case class OnCloseCallback(callback: () => Unit)

  //MatchmakingActor
  sealed trait MathmakingRequest

  case class JoinMatchmaking() extends MathmakingRequest

  case class LeaveMatchmaking() extends MathmakingRequest

}
