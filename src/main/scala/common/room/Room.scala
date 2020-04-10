package common.room

object Room {

  type RoomId = String
  type RoomType = String
  type RoomPassword = String // Must be serializable

  val roomPrivateStatePropertyName = "private"
  val defaultPublicPassword: RoomPassword = ""

  trait BasicRoom {
    val roomId: RoomId

    // We need to override equals so that rooms are compared for their ids
    override def equals(obj: Any): Boolean =
      obj != null && obj.isInstanceOf[BasicRoom] && obj.asInstanceOf[BasicRoom].roomId == this.roomId

    override def hashCode(): Int = this.roomId.hashCode
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





