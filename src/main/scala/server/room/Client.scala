package server.room

import akka.actor.ActorRef
import common.actors.ApplicationActorSystem

/**
 * Minimal interface for a client communication channel. It must have an id to identify it and a send method to
 * send messages
 */
trait Client {
  val id: String

  def send[T](msg: T)
}

object Client {
  /**
   * Creates a client that echoes messages to a specific actor
   *
   * @param id    the id of the client
   * @param actor the actor that will receive the messages
   * @return the client instance
   */
  def fromActor(id: String, actor: ActorRef): Client = new ClientImpl(id, actor)
}

/**
 * A client that echoes messages to an actor
 *
 * @param id          the id of the client
 * @param clientActor the actor that will receive the messages
 */
private class ClientImpl(override val id: String, private val clientActor: ActorRef) extends Client with ApplicationActorSystem {
  override def send[T](msg: T): Unit = this.clientActor ! msg
}

