package common.room

object SharedRoom {

  type RoomId = String
  type RoomType = String
  type RoomPassword = String

  trait RoomState[T] {
    val state: T
  }

  trait BasicRoom {
    val roomId: RoomId
  }

  trait Room extends BasicRoom {

    private var _sharedProperties: Set[RoomProperty] = Set()

    def sharedProperties: Set[RoomProperty] =  _sharedProperties

    def addSharedProperty(property: RoomProperty): Unit = _sharedProperties = _sharedProperties + property
  }

  object Room {
    def apply(id: RoomId): Room = new Room {
      override val roomId: RoomId = id
    }

    val roomPasswordPropertyName = "password"
  }
}





