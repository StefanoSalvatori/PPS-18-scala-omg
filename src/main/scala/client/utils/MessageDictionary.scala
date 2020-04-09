package client.utils

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.ws.Message
import client.room.ClientRoom
import common.room.SharedRoom.{Room, RoomId, RoomPassword, RoomType}
import common.room.{FilterOptions, RoomProperty}

object MessageDictionary {

  //CoreClient

  case class CreatePublicRoom(roomType: RoomType, roomOption: Set[RoomProperty])

  case class CreatePrivateRoom(roomType: RoomType, roomOption: Set[RoomProperty], password: RoomPassword)

  case class GetAvailableRooms(roomType: RoomType, roomOption: FilterOptions)

  case class GetJoinedRooms()

  case class JoinedRooms(joinedRooms: Set[ClientRoom])

  case class ClientRoomActorLeaved(clientRoomActor: ActorRef)

  case class ClientRoomActorJoined(clientRoomActor: ActorRef)

  case class HttpRoomResponse(room: Room)

  case class HttpRoomSequenceResponse(rooms: Seq[Room])

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
   */
  case class HttpSocketRequest(roomId: RoomId)

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

  case class GetClientRoom(joined: Boolean)

  case class ClientRoomResponse(clientRoom: ClientRoom)

  case class SendJoin(roomId: RoomId)

  case class SendLeave()

  case class SendProtocolMessage(msg: Message)

  case class SendStrictMessage(msg: Any with java.io.Serializable)

  case class OnMsg(callback: Any => Unit)

  case class UnknownMessageReply()
}
