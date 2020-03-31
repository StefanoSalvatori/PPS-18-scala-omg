package common

case class RoomProperty(name: String, value: RoomPropertyValue) extends FilterStrategies

case class CustomRoomOptionValue(a: String, b: Int) extends Ordered[CustomRoomOptionValue] {
  override def compare(that: CustomRoomOptionValue): Int = this.b - that.b
}

object BasicRoomPropertyValueConversions {
  implicit def intToIntProperty(value: Int): IntRoomPropertyValue = IntRoomPropertyValue(value)
  implicit def stringToStringProperty(value: String): StringRoomPropertyValue = StringRoomPropertyValue(value)
  implicit def booleanToBooleanProperty(value: Boolean): BooleanRoomPropertyValue = BooleanRoomPropertyValue(value)
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
