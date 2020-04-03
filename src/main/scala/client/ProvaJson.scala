package client

import common.actors.ApplicationActorSystem
import common.{BooleanRoomPropertyValue, EqualStrategy, FilterOptions, IntRoomPropertyValue, NotEqualStrategy, RoomJsonSupport, RoomProperty, StringRoomPropertyValue}
import common.BasicRoomPropertyValueConversions._

object ProvaJson extends App with RoomJsonSupport with ApplicationActorSystem {

  val a = FilterOptions just RoomProperty("a", 2) > 1 andThen RoomProperty("B", true) =:= "a"
  val json = filterOptionsJsonFormat write a
  val unm = filterOptionsJsonFormat read json
  println(a + " " + json + " " + unm)
}
