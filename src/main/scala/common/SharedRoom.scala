package common

import com.typesafe.scalalogging.LazyLogging

object SharedRoom {

  type RoomId = String
  type RoomType = String

  trait Room extends LazyLogging {

    val roomId: RoomId

    import java.lang.reflect.Field
    def valueOf(filedName: String): Any =
      operationOnField(filedName)(field => field get this)

    def `valueOf~AsProperty`(fieldName: String): RoomPropertyValue =
      operationOnField(fieldName)(field => Room.valueToRoomPropertyValue(field get this))

    def propertyOf(propertyName: String): RoomProperty =
      operationOnField(propertyName)(field => RoomProperty(propertyName, Room.valueToRoomPropertyValue(field get this)))

    def setProperties(properties: Set[RoomProperty]): Unit = properties.map(Room.propertyToPair).foreach(property => {
      try {
        operationOnField(property.name)(f => f set (this, property.value))
      } catch {
        case _: NoSuchFieldException =>
          logger debug s"Impossible to set property '${property.name}': No such property in the room."
      }
    })

    private def operationOnField[T](fieldName: String)(f: Function[Field,T]): T = {
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

  private case class PairRoomProperty[T](name: String, value: T)

  object Room {
    def apply(id: RoomId): Room = new Room {
      override val roomId: RoomId = id
    }

    private def propertyToPair[_](property: RoomProperty): PairRoomProperty[_] =
      PairRoomProperty(property.name, property.value match {
        case runtimeValue: IntRoomPropertyValue => runtimeValue.value
        case runtimeValue: StringRoomPropertyValue => runtimeValue.value
        case runtimeValue: BooleanRoomPropertyValue => runtimeValue.value
        case runtimeValue: DoubleRoomPropertyValue => runtimeValue.value
    })

    private def valueToRoomPropertyValue[T](value: T): RoomPropertyValue = value match {
      case v: Int => IntRoomPropertyValue(v)
      case v: String => StringRoomPropertyValue(v)
      case v: Boolean => BooleanRoomPropertyValue(v)
      case v: Double => DoubleRoomPropertyValue(v)
    }
  }
}





