package client

import common.{DoubleRoomPropertyValue, RoomJsonSupport}


object ProvaDouble extends App with RoomJsonSupport {

  val d = DoubleRoomPropertyValue(3)
  val json = doubleRoomPropertyJsonFormat write d
  println(json)
}
