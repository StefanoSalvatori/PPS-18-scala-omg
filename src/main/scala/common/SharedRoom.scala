package common

import com.typesafe.scalalogging.LazyLogging

object SharedRoom {

  type RoomId = String
  type RoomType = String

  trait RoomState[T] {
    val state: T
  }

  import java.lang.reflect.Field
  trait Room extends LazyLogging {

    val roomId: RoomId

    /**
     * Get a field of the room from its name
     *
     * @param fieldName the name of the room property
     * @return the field representing such property
     */
    private def fieldFrom(fieldName: String): Field = {
      this.getClass getDeclaredField fieldName
    }

    def valueOf(fieldName: String): RoomPropertyValue = {
      val field = this fieldFrom fieldName
      field setAccessible true
      val value = (field get this).asInstanceOf[RoomPropertyValue]
      field setAccessible false
      value
    }

    def setProperties(properties: Set[RoomProperty]): Unit = properties.map(tupleFromProperty).foreach(property => {
      try {
        val field = this fieldFrom property._1
        field setAccessible true
        field set(this, property._2)
        field setAccessible false
      } catch {
        case _: NoSuchFieldException =>
          logger debug s"Impossible to set property: No such property ${property._1} in the room."
      }
    })

    private def tupleFromProperty[_](property: RoomProperty): (String, _) = (property.name, property.value match {
      case _: IntRoomPropertyValue => property.value.asInstanceOf[IntRoomPropertyValue].value
      case _: StringRoomPropertyValue => property.value.asInstanceOf[StringRoomPropertyValue].value
      case _: BooleanRoomPropertyValue => property.value.asInstanceOf[BooleanRoomPropertyValue].value
    })
  }

  object Room {
    def apply(id: RoomId): Room = new Room {
      override val roomId: RoomId = id
    }
  }
}





