package client

import akka.actor.{Actor, ActorSystem}
import scala.concurrent.ExecutionContext

trait BasicActor extends Actor {

  implicit val system: ActorSystem = context.system
  implicit val executionContext: ExecutionContext = system.dispatcher

  import MessageDictionary._
  val fallbackReceive: PartialFunction[Any, Unit] = {
    case msg =>
      print("Ignoring unknown message: " + msg)
      sender ! UnknownMessageReply
  }
}
