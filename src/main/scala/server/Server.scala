package server


import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.Materializer
import server.route_service.RouteService

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn


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
