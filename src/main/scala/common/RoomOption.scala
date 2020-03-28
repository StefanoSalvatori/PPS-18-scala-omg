package common

case class RoomOption[T](name: String, value: T) extends FilterStrategies[T]
