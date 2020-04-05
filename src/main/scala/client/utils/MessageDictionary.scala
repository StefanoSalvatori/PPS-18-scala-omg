package client.utils

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCode
import client.room.ClientRoom
import common.{FilterOptions, RoomProperty}
import common.SharedRoom.{Room, RoomId, RoomType}

object MessageDictionary {

  case class CreatePublicRoom(roomType: RoomType, roomOption: Set[RoomProperty])

  case class Join(roomType: RoomType, roomOption: FilterOptions)

  case class JoinById(roomId: RoomId)

  case class GetAvailableRooms(roomType: RoomType, roomOption: FilterOptions)

  case class GetJoinedRooms()

  case class JoinedRooms(joinedRooms: Set[ClientRoom])


  case class RoomLeaved(roomId: RoomId)

  case class RoomResponse(room: Room)

  case class RoomSequenceResponse(rooms: Seq[Room])

  case class FailResponse(ex: Throwable)

  /**
   * Create a room and respond with RoomResponse(Room) or FailResponse on failure
   */
  case class HttpPostRoom(roomType: RoomType, roomOption: Set[RoomProperty])


  /**
   * Get rooms and respond with RoomSequenceResponse(Seq[Room]) or FailResponse on failure
   */
  case class HttpGetRooms(roomType: RoomType, roomOption: FilterOptions)

  case class HttpSocket(roomId: RoomId)


  case class JoinRoom(webSocketUri: String)

  case class LeaveRoom()

  case class SendMsg(msg: String)

  case class OnMsg(callback: String => Unit)

  case class HttpSocketRequest(roomId: RoomId)

  case class HttpSocketSuccess(outRef: ActorRef)

  case class HttpSocketFail(cause: String)

  case class UnknownMessageReply()
}
