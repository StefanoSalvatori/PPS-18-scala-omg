package client.utils

import client.room.ClientRoom
import common.{FilterOptions, RoomProperty}
import common.SharedRoom.{Room, RoomId, RoomType}

object MessageDictionary {

  case class CreatePublicRoom(roomType: RoomType, roomOption: Set[RoomProperty])

  case class Join(roomType: RoomType, roomOption: FilterOptions)

  case class JoinById(roomType: RoomId)

  case class GetAvailableRooms(roomType: RoomType, roomOption: FilterOptions)


  case class GetJoinedRooms()

  case class JoinedRooms(joinedRooms: Set[ClientRoom])

  case class RoomResponse(room: Room)

  case class RoomSequenceResponse(room: Seq[Room])

  case class FailResponse(ex: Throwable)


  /**
   * Create a room and respond with RoomResponse(Room) or FailResponse on failure
   */
  case class HttpPostRoom(roomType: RoomType, roomOption: Set[RoomProperty])


  /**
   * Get rooms and respond with RoomSequenceResponse(Seq[Room]) or FailResponse on failure
   */
  case class HttpGetRooms(roomType: RoomType, roomOption: FilterOptions)



  case class UnknownMessageReply()
}
