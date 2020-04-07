package common

case class RoomProperty(name: String, value: RoomPropertyValue) extends FilterStrategies

object BasicRoomPropertyValueConversions {
  implicit def intToIntProperty(value: Int): IntRoomPropertyValue = IntRoomPropertyValue(value)
  implicit def stringToStringProperty(value: String): StringRoomPropertyValue = StringRoomPropertyValue(value)
  implicit def booleanToBooleanProperty(value: Boolean): BooleanRoomPropertyValue = BooleanRoomPropertyValue(value)
  implicit def DoubleToBooleanProperty(value: Double): DoubleRoomPropertyValue = DoubleRoomPropertyValue(value)
}

trait RoomPropertyValue { self =>
  def compare(that: self.type): Int
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
