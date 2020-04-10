package client.examples

import client.Client

import scala.concurrent.ExecutionContext
import common.room._
import common.room.RoomPropertyValueConversions._

import scala.util.Success // Implicit conversions Int -> IntRoomPropertyValue etc.

// Filters on basic room option values: Int, String, Boolean, Double
object SimpleFilters extends App {

  val prop1 = RoomProperty("A", 3) // Int room option
  val prop2 = RoomProperty("B", "svv") // String room option
  val prop3 = RoomProperty("C", true) // Boolean room option
  val prop4 = RoomProperty("D", 0.1) // Double room option
  // Create simple filter option
  val simple = prop1 =!= 2
  // Create filter options, i.e. concatenation of clauses
  val empty = FilterOptions.empty // Empty filter
  val short = FilterOptions just prop4 > 0.5 // Filter with 1 clause
  val filters = FilterOptions just prop1 > 2 andThen prop2 =!= "asc" andThen simple // Filter with more concatenated clauses
  val combined = short ++ filters ++ empty // Combine more filters in one single filter (union of the clauses)

  println(empty.options)
  println(short.options)
  println(filters.options)
  println(combined.options)
}

  // Given a room, use its properties in filters
  /*
  import common.SharedRoom._
  class MyRoom(override val roomId: String) extends Room {
    val a: Int = 3
    val b: String = "svv"
    val c: Boolean = true
  }
  val room = new MyRoom("id")

  implicit val executionContext: ExecutionContext = ExecutionContext.global
  val client = Client("localhost", 8080) //scalastyle:ignore magic.number
  client createPublicRoom "test_room" onComplete {
    case Success(room) =>
      val filter = room.propertyOf("a") > 0 andThen room.propertyOf("b") =:= "abc"
      println(filter)
  }
}