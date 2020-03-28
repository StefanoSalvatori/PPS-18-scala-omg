package client

import common.Room

object MessageDictionary {

  case class CreatePublicRoom(roomType: String)
  case class NewJoinedRoom(room: Room)
  case class GetJoinedRooms()
  case class JoinedRooms(rooms: Set[Room])

  case class UnknownMessageReply()
}
