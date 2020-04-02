package server

import common._
import common.RoomPropertyValue

case class MyRoomPropertyValue(a: String, b: Int) extends RoomPropertyValue {
  override def compare(that: this.type): Int = this.b - that.b
}

class Room(val id: String) {

  var A = IntRoomPropertyValue(0)
  def a(x: IntRoomPropertyValue): Unit = A = x
  var B = StringRoomPropertyValue("abc")
  var C = MyRoomPropertyValue("a", 1)
}

class RoomManagerService {

  var rooms: Set[Room] = Set[Room]()
  import common.BasicRoomPropertyValueConversions._
  val r1 = new Room("room1")
  val r2 = new Room("room2")
  r1 a 1
  r2 a 2
  rooms = rooms + r1
  rooms = rooms + r2

  def filterRooms(filter: FilterOptions): Set[Room] =
    rooms.filter(room => {
       // Given a room, check if such room satisfies all filter constraints
       filter.options.forall(filterOption => {
         try {
           val field = room.getClass getDeclaredField filterOption.optionName

           field setAccessible true

           val value = (field get room).asInstanceOf[RoomPropertyValue]
           val filterValue = filterOption.value.asInstanceOf[value.type]

           field setAccessible false

           filterOption.strategy evaluate (value, filterValue)
         } catch {
           // A room is dropped if it doesn't contain the specified field to be used in the filter
           case _: NoSuchFieldException => false
         }
       })
    })
}

object FilterHandling extends App {

  val manager = new RoomManagerService()

  import common.BasicRoomPropertyValueConversions._
  val prop1 = RoomProperty("A", 1)
  val prop2 = RoomProperty("B", "abc")
  val prop3 = RoomProperty("C", MyRoomPropertyValue("b", 1))
  //val filter = FilterOptions just prop1 =:= 1 andThen prop2 =:= "abc"
  val filter = FilterOptions just prop3 =:= MyRoomPropertyValue("cd", 1) andThen prop1 > 1
  val filteredRooms = manager filterRooms filter
  println(filteredRooms)
}
