package common.room

import java.util.NoSuchElementException

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

  trait Room {
    val roomId: RoomId

    def properties: Set[RoomProperty]

    /**
     * Conversion utility to handle properties
     *
     * @return a map with property name as key and RoomPropertyValue as value
     */
    def propertiesAsMap: Map[String, RoomPropertyValue] =
      this.properties.map(p => (p.name, p.value)).toMap[String, RoomPropertyValue]

    /**
     * Conversion utility to handle properties
     *
     * @return a map with property name as key and the value of the property as value
     */
    def propertyValues: Map[String, Any] = propertiesAsMap.map(e => (e._1, RoomPropertyValue valueOf e._2))


    // We need to override equals so that rooms are compared for their ids
    override def equals(obj: Any): Boolean =
      obj != null && obj.isInstanceOf[BasicRoom] && obj.asInstanceOf[BasicRoom].roomId == this.roomId

    override def hashCode(): Int = this.roomId.hashCode
  }

  trait BasicRoom extends Room {
    /**
     * Getter of the value of a given property
     *
     * @param propertyName the name of the property
     * @throws NoSuchPropertyException if the requested property does not exist
     * @return the value of the property, as instance of first class values (Int, String, Boolean, Double)
     */
    def valueOf(propertyName: String): Any

    /**
     * Getter of a room property
     *
     * @param propertyName The name of the property
     * @throws NoSuchPropertyException if the requested property does not exist
     * @return The selected property
     */
    def propertyOf(propertyName: String): RoomProperty

  }

  case class SharedRoom(override val roomId: RoomId, override val properties: Set[RoomProperty]) extends Room

}





