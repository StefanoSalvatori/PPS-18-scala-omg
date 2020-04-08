package client.examples

import common.room

object FilterUsage extends App {

  // Filters on basic room option values: Int, String, Boolean
  import common.room.BasicRoomPropertyValueConversions._
  import common.room.{FilterOptions, RoomProperty} // Implicit conversions Int -> IntRoomPropertyValue etc.
  val prop1 = room.RoomProperty("A", 3) // Int room option
  val prop2 = room.RoomProperty("B", "svv") // String room option
  val prop3 = room.RoomProperty("C", true) // Boolean room option
  // Create simple filter option
  val simple = prop1 =!= 2
  // Create filter options, i.e. concatenation of clauses
  val empty = FilterOptions.empty // Empty filter
  val short = FilterOptions just prop1 > 2 // Filter with 1 clause
  val filters = FilterOptions just prop1 > 2 andThen prop2 =!= "asc" andThen simple // Filter with more concatenated clauses
  val combined = short ++ filters ++ empty // Combine more filters in one single filter (union of the clauses)

  println(empty.options)
  println(short.options)
  println(filters.options)
  println(combined.options)

  // Given a room, use its properties in filters
  /*
  import common.SharedRoom._
  class MyRoom(override val roomId: String) extends Room {
    val a: Int = 3
    val b: String = "svv"
    val c: Boolean = true
  }
  val room = new MyRoom("id")

  val filter = FilterOptions just room.propertyOf("a") > 1
  */
}