package client.examples

object FilterUsage extends App {

  // Filters on basic room option values: Int, String, Boolean
  import common.BasicRoomPropertyValueConversions._ // Implicit conversions Int -> IntRoomPropertyValue etc.
  import common.RoomProperty
  val prop1 = RoomProperty("A", 3) // Int room option
  val prop2 = RoomProperty("B", "svv") // String room option
  val prop3 = RoomProperty("C", true) // Boolean room option

  import common.FilterOptions
  // Create simple filter option
  val simple = prop1 =!= 2
  // Create filter options, i.e. concatenation of clauses
  val empty = FilterOptions.empty() // Empty filter
  val short = FilterOptions just prop1 > 2 // Filter with 1 clause
  val filters = FilterOptions just prop1 > 2 andThen prop2 =!= "asc" andThen simple // Filter with more concatenated clauses
  val combined = short ++ filters ++ empty // Combine more filters in one single filter (union of the clauses)

  println(empty.options)
  println(short.options)
  println(filters.options)
  println(combined.options)
}