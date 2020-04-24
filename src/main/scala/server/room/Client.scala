package server.room

import akka.actor.ActorRef

/**
 * Minimal interface for a client communication channel. It must have an id to identify it and a send method to
 * send messages
 */
trait Client {
  val id: String

  def send[T](msg: T)

  override def equals(obj: Any): Boolean = obj != null && obj.isInstanceOf[Client] && obj.asInstanceOf[Client].id == this.id

  override def hashCode(): Int = super.hashCode()
}

object Client {
  /**
   * Creates a client that echoes messages to a specific actor
   *
   * @param id    the id of the client
   * @param actor the actor that will receive the messages
   * @return the client instance
   */
  def asActor(id: String, actor: ActorRef): Client = new ClientImpl(id, actor)

  /**
   * It creates a mocked client that can't send any message
   * @param id the id of the client; if not provided it will have an empty one
   * @return the client instance
   */
  def mock(id: String = ""): Client = MockClient(id)
}

/**
 * A client that echoes messages to an actor
 *
 * @param id          the id of the client
 * @param clientActor the actor that will receive the messages
 */

private class ClientImpl(override val id: String, private val clientActor: ActorRef) extends Client {
  override def send[T](msg: T): Unit = this.clientActor ! msg
}

private case class MockClient(override val id: String) extends Client {
  override def send[T](msg: T): Unit = {}
}
