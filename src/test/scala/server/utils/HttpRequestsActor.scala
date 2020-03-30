package server.utils


import akka.actor.{Actor, ActorSystem, Props}
import akka.http.impl.settings.HostConnectionPoolSetup
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.HostConnectionPool
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.ExecutionContextExecutor

object HttpRequestsActor {
  case class Request(request: HttpRequest)

  def apply(): Props = Props(classOf[HttpRequestsActor])

}

class HttpRequestsActor extends Actor {

  import server.utils.HttpRequestsActor._

  implicit val actorSystem: ActorSystem = context.system
  implicit val executionContext: ExecutionContextExecutor = context.dispatcher

  override def receive: Receive = {
    case Request(httpRequest) =>
      import akka.pattern.pipe
      Http(context.system).singleRequest(httpRequest) pipeTo sender
  }
}
