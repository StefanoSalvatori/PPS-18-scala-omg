package common.room

object Room {

  type RoomId = String
  type RoomType = String
  type RoomPassword = String // Must be serializable

  val roomPrivateStatePropertyName = "private"
  val defaultPublicPassword: RoomPassword = ""

  trait RoomWithId {
    val roomId: RoomId
  }

  trait BasicRoom extends RoomWithId {

    /**
     * Getter of the value of a given property
     * @param propertyName the name of the property
     * @return the value of the property, as instance of first class values (Int, String, Boolean, Double)
     */
    def valueOf(propertyName: String): Any

    /**
     * Getter of a room property
     * @param propertyName The name of the property
     * @return The selected property
     */
    def propertyOf(propertyName: String): RoomProperty

    // We need to override equals so that rooms are compared for their ids
    override def equals(obj: Any): Boolean =
      obj != null && obj.isInstanceOf[BasicRoom] && obj.asInstanceOf[BasicRoom].roomId == this.roomId

    override def hashCode(): Int = this.roomId.hashCode
  }

  trait SharedRoom extends RoomWithId {

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





