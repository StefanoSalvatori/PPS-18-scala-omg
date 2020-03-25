package client

import MessageDictionary._

sealed trait Client {
  def shutdown(): Unit
}

object Client {
  def apply(serverAddress: String, serverPort: Int): ClientImpl = new ClientImpl(serverAddress, serverPort)
}

class ClientImpl(private val serverAddress: String, private val serverPort: Int) extends Client {

  private val serverUri = "http://" + serverAddress + ":" + serverPort

  import akka.actor.ActorSystem
  import com.typesafe.config.ConfigFactory
  private val system = ActorSystem("ClientSystem", ConfigFactory.load())
  private val clientActor = system actorOf ClientActor(serverUri)

  override def shutdown(): Unit = system.terminate()
}
