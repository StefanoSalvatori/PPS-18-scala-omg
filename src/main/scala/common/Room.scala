package common

object SharedRoom {

  type RoomId = String
  type RoomType = String

  trait RoomState[T] {
    this: Room =>
    val state: T
  }

  trait Room {
    val roomId: RoomId
  }


  object Room {
    def apply(id: RoomId): Room = new Room {
      override val roomId: RoomId = id
    }
  }
}





