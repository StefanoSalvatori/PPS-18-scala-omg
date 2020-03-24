package client

import akka.actor.Actor
sealed trait ClientActor extends Actor

object ClientActor {
  import akka.actor.Props
  def apply(): Props = Props(classOf[ClientActorImpl])
}

class ClientActorImpl extends ClientActor {

  import MessageDictionary._
  override def receive: Receive = {
    case _ =>
      print("Ignoring unknown message: " + _)
      sender ! UnknownMessageReply
  }
}