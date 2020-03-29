package client

import client.room.ClientRoom.ClientRoom
import server.room.ServerRoom.RoomType

object MessageDictionary {

  case class CreatePublicRoom()

  case class JoinOrCreate(roomType: RoomType, roomOption: Any)

  case class GetAvailableRooms(roomType: RoomType)

  case class NewJoinedRoom(roomId: String)

  case class GetJoinedRooms()

  case class JoinedRooms(rooms: Set[ClientRoom])

  case class UnknownMessageReply()
}
