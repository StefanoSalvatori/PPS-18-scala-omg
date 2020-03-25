package client

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import client.MessageDictionary._

class CoreClientSpec extends TestKit(ActorSystem("ClientSystem", ConfigFactory.load()))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {

  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

  "A core client" must {
    "ignore an unknown message" in {
      val testActor = system actorOf CoreClient("http://localhost:8080")
      testActor ! "random message"
      expectMsg(UnknownMessageReply)
    }
  }
}

