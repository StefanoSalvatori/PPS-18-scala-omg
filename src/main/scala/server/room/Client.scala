package server.room

import akka.actor.ActorRef

/**
 * Minimal interface for a client communication channel. It must have an Id and a send method.
 */
sealed trait Client {

  /**
   * Client identifier.
   */
  val id: String

  /**
   * Send a message to such client.
   * @param msg the message to send
   * @tparam T the type of the message to send
   */
  def send[T](msg: T)

  // Comparing clients by Id
  override def equals(obj: Any): Boolean =
    obj != null && obj.isInstanceOf[Client] && obj.asInstanceOf[Client].id == this.id

  override def hashCode(): Int = super.hashCode()
}

object Client {

  /**
   * Creates a client that echoes messages to a specific actor.
   * @param id    the id of the client
   * @param actor the actor that will receive the messages
   * @return the client instance
   */
  def asActor(id: String, actor: ActorRef): Client = new ClientImpl(id, actor)

  /**
   * It creates a mocked client that may have an Id and that can't send any message.
   * @param id the id of the client; if not provided it will have an empty one
   * @return the client instance
   */
  def mock(id: String = ""): Client = MockClient(id)
}

private class ClientImpl(override val id: String, private val clientActor: ActorRef) extends Client {
  override def send[T](msg: T): Unit = clientActor ! msg
}

private case class MockClient(override val id: String) extends Client {
  override def send[T](msg: T): Unit = {}
}
