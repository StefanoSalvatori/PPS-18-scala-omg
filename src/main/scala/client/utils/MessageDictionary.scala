package client

import akka.actor.ActorRef
import client.room.ClientRoom
import common.CommonRoom.{Room, RoomId, RoomType}

object MessageDictionary {

  case class CreatePublicRoom(roomType: RoomType, roomOption: Any)

  case class Join(roomType: RoomType, roomOption: Any)

  case class JoinById(roomType: RoomId)

  case class GetAvailableRooms(roomType: RoomType, roomOption: Any)

  case class GetJoinedRooms()

  case class JoinedRooms(joinedRooms: Set[ClientRoom])

  case class RoomResponse(room: Room)

  case class RoomSequenceResponse(room: Seq[Room])

  case class FailResponse(ex: Throwable)





  case class HttpPostRoom(roomType: RoomType, roomOption: Any)

  case class HttpGetRooms(roomType: RoomType, roomOption: Any)



  case class UnknownMessageReply()
}
