package common

case class RoomProperty[T:Ordering](name: String, value: T) extends FilterStrategies[T]

object BasicRoomOptionValueComparators {
  implicit object IntOrdering extends Ordering[Int] {
    override def compare(x: Int, y: Int): Int = x compareTo y
  }

  implicit object StringOrdering extends Ordering[String] {
    override def compare(x: String, y: String): Int = x compareTo y
  }

  implicit object BooleanOrdering extends Ordering[Boolean] {
    override def compare(x: Boolean, y: Boolean): Int = x compareTo y
  }
}

case class CustomRoomOptionValue(a: String, b: Int) extends Ordered[CustomRoomOptionValue] {
  override def compare(that: CustomRoomOptionValue): Int = this.b - that.b
}