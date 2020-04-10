package common.room

object Room {

  type RoomId = String
  type RoomType = String
  type RoomPassword = String

  val roomPrivateStatePropertyName = "private"
  val defaultPublicPassword: RoomPassword = ""

  trait BasicRoom {
    val roomId: RoomId
  }

  trait SharedRoom extends BasicRoom {

    private var _sharedProperties: Set[RoomProperty] = Set()

    def sharedProperties: Set[RoomProperty] =  _sharedProperties

    def addSharedProperty(property: RoomProperty): Unit = _sharedProperties = _sharedProperties + property
  }

  object SharedRoom {
    def apply(id: RoomId): SharedRoom = new SharedRoom {
      override val roomId: RoomId = id
    }

    val roomPasswordPropertyName = "password"
  }
}





