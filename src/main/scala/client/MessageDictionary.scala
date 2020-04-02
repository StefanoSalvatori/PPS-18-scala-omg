package client

import client.room.ClientRoom.ClientRoom
import common.CommonRoom.RoomType

object MessageDictionary {

  case class CreatePublicRoom(roomType: RoomType, roomOption: Any)

  case class JoinOrCreate(roomType: RoomType, roomOption: Any)

  case class GetAvailableRooms(roomType: RoomType)


  case class NewJoinedRoom(roomId: ClientRoom)

  case class GetJoinedRooms()

  case class JoinedRooms(rooms: Set[ClientRoom])

  case class UnknownMessageReply()
}
