package client

import common.actors.ApplicationActorSystem
import common.{BooleanRoomPropertyValue, EqualStrategy, IntRoomPropertyValue, RoomJsonSupport, RoomProperty, StringRoomPropertyValue}
import common.BasicRoomPropertyValueConversions._

object ProvaJson extends App with RoomJsonSupport with ApplicationActorSystem {

  val a = RoomProperty("a", true)
  val json = roomPropertyJsonFormat write a
  val unm = roomPropertyJsonFormat read json
  println(a + " " + json + " " + unm)
}
