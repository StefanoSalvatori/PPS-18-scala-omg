package client.room

import server.room.ServerRoom.RoomId

object ClientRoom {

  trait ClientRoom <: {
    val roomId: String

    def join(): Any
    def leave(): Any
  }

  object ClientRoom{
    def apply(roomId: RoomId): ClientRoom = ClientRoomImpl(roomId)
  }

  private case class ClientRoomImpl(roomId: RoomId) extends ClientRoom {
    override def join() = {}

    override def leave() = {}
  }

}
