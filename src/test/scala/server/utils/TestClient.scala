package server.utils

import java.util.UUID

import server.room.Client

object TestClient {
  def apply(): TestClient = new TestClient(UUID.randomUUID.toString)
}

/**
 * A client that expose the last message received.
 */
case class TestClient(override val id: String) extends Client {
  private var messageReceived: Option[Any] = Option.empty

  def lastMessageReceived: Option[Any] = this.messageReceived

  override def send[T](msg: T): Unit = this.messageReceived = Option(msg)
}

