package scalaomg.common.room

case class RoomProperty(name: String, value: RoomPropertyValue) extends FilterStrategies

trait RoomPropertyValue { self =>
  def compare(that: self.type): Int
}

object RoomPropertyValue {
  def valueOf(propertyValue: RoomPropertyValue): Any = propertyValue match {
    case v: IntRoomPropertyValue => v.value
    case v: StringRoomPropertyValue => v.value
    case v: BooleanRoomPropertyValue => v.value
    case v: DoubleRoomPropertyValue => v.value
  }

  // Useful when we can't directly instantiate the property value since we don't know the type of the value
  def propertyValueFrom[T](value: T): RoomPropertyValue = value match {
    case v: Int => IntRoomPropertyValue(v)
    case v: String => StringRoomPropertyValue(v)
    case v: Boolean => BooleanRoomPropertyValue(v)
    case v: Double => DoubleRoomPropertyValue(v)
  }
}

case class IntRoomPropertyValue(value: Int) extends RoomPropertyValue {
  override def compare(that: this.type): Int = this.value - that.value
}

case class StringRoomPropertyValue(value: String) extends RoomPropertyValue {
  override def compare(that: this.type): Int = this.value compareTo that.value
}

case class BooleanRoomPropertyValue(value: Boolean) extends RoomPropertyValue {
  override def compare(that: this.type): Int = this.value compareTo that.value
}

case class DoubleRoomPropertyValue(value: Double) extends RoomPropertyValue {
  override def compare(that: this.type): Int = this.value compareTo that.value
}

object RoomPropertyValueConversions {
  implicit def intToIntProperty(value: Int): IntRoomPropertyValue = IntRoomPropertyValue(value)
  implicit def stringToStringProperty(value: String): StringRoomPropertyValue = StringRoomPropertyValue(value)
  implicit def booleanToBooleanProperty(value: Boolean): BooleanRoomPropertyValue = BooleanRoomPropertyValue(value)
  implicit def DoubleToBooleanProperty(value: Double): DoubleRoomPropertyValue = DoubleRoomPropertyValue(value)
}

case class NoSuchPropertyException(
  private val message: String = "The specified property does not exist in the room",
  private val cause: Throwable = None.orNull
                                  ) extends Exception(message, cause)