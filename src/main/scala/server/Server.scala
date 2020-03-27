package server


import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.Materializer
import server.route_service.RouteService

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn

object Test{

  val Address: String = "localhost"
  val Port: Int = 8080

  def main(args: Array[String]): Unit = {
    val l1 = List(1,2,3)
    val l2 = List(4,5,6)
    val l3 = List(7,8,9)
    val l = Map("l1" -> l1, "l2" -> l2, "l3" -> l3)

    val lr = l.values.flatMap(_.toList).toList
    println(lr)
  }

  }

//Main
object Server{

  val Address: String = "localhost"
  val Port: Int = 8080

  def main(args: Array[String]) {
    implicit val system = ActorSystem("Server")
    implicit val materializer = Materializer
    // needed for the future flatMap/onComplete to stop server
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher


    val s: Future[Http.ServerBinding] = Http().bindAndHandle(RouteService().route, Address, Port)

    println(s"Server online at http://$Address:$Port/" +
      s"\nPress RETURN to stop...")

    StdIn.readLine() // let it run until user presses return
    s.flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done

    println(s"Server closed")
  }
}
