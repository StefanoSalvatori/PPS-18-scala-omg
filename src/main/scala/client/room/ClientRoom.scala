package client.room

import common.CommonRoom.{Room, RoomId}


object ClientRoom {

  trait ClientRoom extends Room {

    def join(): Any
    def leave(): Any
  }

  object ClientRoom{
    def apply(roomId: RoomId): ClientRoom = ClientRoomImpl(roomId)
  }

  case class ClientRoomImpl(roomId: RoomId) extends ClientRoom {
    override def join() = {}

    override def leave() = {}
  }

}
