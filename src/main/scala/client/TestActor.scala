package client
import akka.actor.{Actor, Props}

sealed trait TestActor extends Actor

object TestActor {
  def apply(): Props = Props(classOf[TestActorImpl])
}

class TestActorImpl extends TestActor {

  override def receive: Receive = {
    case msg =>
      print("Received: " + msg)
      sender ! "Reply"
  }
}