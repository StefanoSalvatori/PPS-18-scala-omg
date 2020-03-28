package client

object ClientStub extends App {

  //private val serverAddress = "localhost"
  //private val serverPort = 8080
  //val client = Client(serverAddress, serverPort)

  //client createPublicRoom "test_room"

  //client.shutdown()


  import common.RoomOption
  import common.FilterOptions

  val prop1 = RoomOption("A", 3)
  val prop2 = RoomOption("B", "svv")
  val prop3 = RoomOption("C", true)

  //val empty = FilterOptions.empty()
  //val short = FilterOptions just prop1 > 2
  //val filters = (prop1 > 2) andThen (prop2 :!= "asc") andThen (prop3 := true)
  //val combined = short ++ filters ++ empty

  //FilterOptions just prop2 := true

  //println(empty.options)
  //println(short.options)
  //println(filters.options)
  //println(combined.options)
}
