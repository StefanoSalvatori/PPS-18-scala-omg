package client

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import client.MessageDictionary._

class HttpClientSpec extends TestKit(ActorSystem("ClientSystem", ConfigFactory.load()))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {

  private val serverUri = "http://localhost:8080"

  private val requestTimeout = 5 // Seconds
  import akka.util.Timeout
  implicit val timeout: Timeout = Timeout(requestTimeout, TimeUnit.SECONDS)

  override def afterAll: Unit = TestKit.shutdownActorSystem(system)

  "An Http client actor" must {

    val probe = TestProbe()
    val httpTestActor: ActorRef = system actorOf HttpClient(serverUri, probe.ref)

    "when asked to create a new public room, return the new room" in {
      httpTestActor ! CreatePublicRoom
      probe expectMsgClass classOf[NewJoinedRoom]
    }
  }

}
