package common.room

import com.typesafe.scalalogging.LazyLogging

object SharedRoom {

  type RoomId = String
  type RoomType = String
  type RoomPassword = String

  trait RoomState[T] {
    val state: T
  }

  trait Room extends LazyLogging {
    val roomId: RoomId
  }

  object Room {
    def apply(id: RoomId): Room = new Room {
      override val roomId: RoomId = id
    }

    val roomPasswordPropertyName = "password"
  }
}





