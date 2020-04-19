package common.room

object Room {

  type RoomId = String
  type RoomType = String
  type RoomPassword = String // Must be serializable

  /**
   * Name of the room property "private state". Every room possesses it by default.
   */
  val roomPrivateStatePropertyName = "private"

  /**
   * Name of the property used for exchanging password between clients and server.
   */
  val roomPasswordPropertyName = "password"

  val defaultPublicPassword: RoomPassword = ""

  trait RoomWithId {
    val roomId: RoomId
  }

  trait BasicRoom extends RoomWithId {

    /**
     * Getter of the value of a given property
     * @param propertyName the name of the property
     * @throws NoSuchPropertyException if the requested property does not exist
     * @return the value of the property, as instance of first class values (Int, String, Boolean, Double)
     */
    def valueOf(propertyName: String): Any

    /**
     * Getter of a room property
     * @param propertyName The name of the property
     * @throws NoSuchPropertyException if the requested property does not exist
     * @return The selected property
     */
    def propertyOf(propertyName: String): RoomProperty

    // We need to override equals so that rooms are compared for their ids
    override def equals(obj: Any): Boolean =
      obj != null && obj.isInstanceOf[BasicRoom] && obj.asInstanceOf[BasicRoom].roomId == this.roomId

    override def hashCode(): Int = this.roomId.hashCode
  }

  case class SharedRoom(override val roomId: RoomId, sharedProperties: Set[RoomProperty]) extends RoomWithId
}





