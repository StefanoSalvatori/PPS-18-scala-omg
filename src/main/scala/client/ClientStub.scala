package client

object ClientStub extends App {

  //private val serverAddress = "localhost"
  //private val serverPort = 8080
  //val client = Client(serverAddress, serverPort)

  //client createPublicRoom "test_room"

  //client.shutdown()


  import common.MyOpt
  import common.FilterOptions

  val prop1 = MyOpt[Int]("A", 3)
  val prop2 = MyOpt[String]("B", "svv")
  val prop3 = MyOpt[Boolean]("C", true)

  val filters = (prop1 > 2) andThen (prop2 :!= "asc") andThen (prop3 := true)
  val shortFilter = FilterOptions.just(prop1 > 2)

  println(filters.options)
  println(shortFilter.options)
}
