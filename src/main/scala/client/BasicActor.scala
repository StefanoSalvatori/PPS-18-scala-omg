package client

import akka.actor.{Actor, ActorSystem}
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.ExecutionContext

trait BasicActor extends Actor with LazyLogging {

  implicit val system: ActorSystem = context.system
  implicit val executionContext: ExecutionContext = system.dispatcher

  val fallbackReceive: PartialFunction[Any, Unit] = {
    case msg => logger debug s"Ignoring unknown message: $msg"
  }
}
