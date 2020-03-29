package client.examples

object FilterUsage extends App {

  import common.RoomOption
  val prop1 = RoomOption("A", 3) // Int room option
  val prop2 = RoomOption("B", "svv") // String room option
  val prop3 = RoomOption("C", true) // Boolean room option

  import common.FilterOptions
  val empty = FilterOptions.empty() // Empty filter
  val short = FilterOptions just prop1 > 2 // Filter with 1 clause
  val filters = (prop1 > 2) andThen (prop2 :!= "asc") andThen (prop3 := true) // Filter with more concatenated clauses
  val combined = short ++ filters ++ empty // Combine more filters in one single filter (union of the clauses)

  println(empty.options)
  println(short.options)
  println(filters.options)
  println(combined.options)
}
