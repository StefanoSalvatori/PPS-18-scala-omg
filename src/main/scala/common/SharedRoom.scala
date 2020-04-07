package common

import com.typesafe.scalalogging.LazyLogging

object SharedRoom {

  type RoomId = String
  type RoomType = String

  trait RoomState[T] {
    val state: T
  }

  trait Room extends LazyLogging {

    val roomId: RoomId

    import java.lang.reflect.Field
    def valueOf(filedName: String): Any =
      operationOnField(filedName)(field => field get this)

    def `valueOf~AsProperty` (fieldName: String): RoomPropertyValue =
      operationOnField(fieldName)(field => Room.valueToRoomPropertyValue(field get this))

    def setProperties(properties: Set[RoomProperty]): Unit = properties.map(Room.tupleFromProperty).foreach(property => {
      try {
        operationOnField(property._1)(f => f set (this, property._2))
      } catch {
        case _: NoSuchFieldException =>
          logger debug s"Impossible to set property '${property._1}': No such property in the room."
      }
    })

    private def operationOnField[T](fieldName: String)(f: Field => T): T = {
      val field = this fieldFrom fieldName
      field setAccessible true
      val result = f(field)
      field setAccessible false
      result
    }

    private def fieldFrom(fieldName: String): Field = {
      this.getClass getDeclaredField fieldName
    }
  }

  object Room {
    def apply(id: RoomId): Room = new Room {
      override val roomId: RoomId = id
    }

    def tupleFromProperty[_](property: RoomProperty): (String, _) = (property.name, property.value match {
      case _: IntRoomPropertyValue => property.value.asInstanceOf[IntRoomPropertyValue].value
      case _: StringRoomPropertyValue => property.value.asInstanceOf[StringRoomPropertyValue].value
      case _: BooleanRoomPropertyValue => property.value.asInstanceOf[BooleanRoomPropertyValue].value
    })

    def valueToRoomPropertyValue[T](value: T): RoomPropertyValue = value match {
      case v: Int => IntRoomPropertyValue(v)
      case v: String => StringRoomPropertyValue(v)
      case v: Boolean => BooleanRoomPropertyValue(v)
    }
  }
}





