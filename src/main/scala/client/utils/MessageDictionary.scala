package client.utils

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCode
import client.room.ClientRoom
import common.{FilterOptions, RoomProperty}
import common.SharedRoom.{Room, RoomId, RoomType}

object MessageDictionary {

  //CoreClient

  case class CreatePublicRoom(roomType: RoomType, roomOption: Set[RoomProperty])

  case class Join(roomType: RoomType, roomOption: FilterOptions)

  case class JoinById(roomId: RoomId)

  case class GetAvailableRooms(roomType: RoomType, roomOption: FilterOptions)

  case class GetJoinedRooms()

  case class JoinedRooms(joinedRooms: Set[ClientRoom])

  case class ClientRoomLeaved(roomId: RoomId)

  case class ClientRoomJoined(room: ClientRoom)

  case class HttpRoomResponse(room: Room)

  case class HttpRoomSequenceResponse(rooms: Seq[Room])

  case class FailResponse(ex: Throwable)


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
   * Perform a web socket request to open a connection to server side room with the gicen id.
   * If the connection is successful respond with message [[HttpSocketSuccess]] otherwise [[HttpSocketFail]]
   * @param roomId id of the room to connect to
   */
  case class HttpSocketRequest(roomId: RoomId)

  /**
   * Successful response of an [[HttpSocketRequest]].
   * Contains an actor ref.
   * @param outRef Sending messages to this actor means sending them in the socket
   */
  case class HttpSocketSuccess(outRef: ActorRef)

  /**
   * Failure response of an [[HttpSocketRequest]].
   * @param cause what caused the failure
   */
  case class HttpSocketFail(cause: String)


  //ClientRoomActor

  case class GetClientRoom(joined: Boolean)

  case class ClientRoomResponse(clientRoom: ClientRoom)

  case class JoinRoom(webSocketUri: String)

  case class LeaveRoom()

  case class SendMsg(msg: String)

  case class OnMsg(callback: String => Unit)



  case class UnknownMessageReply()
}
