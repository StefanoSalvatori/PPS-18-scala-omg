package common

trait Room <: {
  val roomId: String
}

case class SimpleRoomWithId(roomId: String) extends Room

object Room {
  def apply(roomId: String): Room = SimpleRoomWithId(roomId)
}