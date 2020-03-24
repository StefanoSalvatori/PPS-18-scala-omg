package client

object Client extends App {

  import akka.actor.ActorSystem
  import com.typesafe.config.ConfigFactory
  val sys = ActorSystem("ClientSystem", ConfigFactory.load())

  val ref1 = sys.actorOf(TestActor(), "testActor")
  ref1 ! "ping"

  sys stop ref1
  sys.terminate()
}
