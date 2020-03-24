package client

sealed trait Client {
  def shutdown(): Unit
}

object Client {
  def apply(): ClientImpl = new ClientImpl()
}

class ClientImpl extends Client {

  import akka.actor.ActorSystem
  import com.typesafe.config.ConfigFactory
  private val system = ActorSystem("ClientSystem", ConfigFactory.load())
  //private val clientActor = system actorOf ClientActor()

  override def shutdown(): Unit = system.terminate()
}
