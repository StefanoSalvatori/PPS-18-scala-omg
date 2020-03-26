package client

import common.Room

object MessageDictionary {

  case class CreatePublicRoom()
  case class NewJoinedRoom(roomId: String)
  case class GetJoinedRooms()
  case class JoinedRooms(rooms: Set[Room])

  case class UnknownMessageReply()
}
