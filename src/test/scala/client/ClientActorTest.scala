package client

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class ClientActorTest extends TestKit(ActorSystem("ClientSystem", ConfigFactory.load()))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {

  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

  import client.MessageDictionary._
  "A client actor" must {
    "ignore an unknown message" in {
      val testActor = system actorOf ClientActor()
      testActor ! "random message"
      expectMsg(UnknownMessageReply)
    }
  }
}

