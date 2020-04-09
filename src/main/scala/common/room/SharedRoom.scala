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
    private var _properties: Set[RoomProperty] = Set()

    def properties: Set[RoomProperty] =  _properties

    def addProperty(property: RoomProperty): Unit = _properties = _properties + property
  }

  object Room {
    def apply(id: RoomId): Room = new Room {
      override val roomId: RoomId = id
    }

    val roomPasswordPropertyName = "password"
  }
}





