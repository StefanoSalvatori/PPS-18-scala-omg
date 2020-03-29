package client

import client.room.ClientRoom.ClientRoom

object MessageDictionary {

  case class CreatePublicRoom()
  case class NewJoinedRoom(roomId: String)
  case class GetJoinedRooms()
  case class JoinedRooms(rooms: Set[ClientRoom])

  case class UnknownMessageReply()
}
