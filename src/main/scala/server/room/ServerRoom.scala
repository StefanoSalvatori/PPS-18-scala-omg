package server.room

import common.CommonRoom.Room


trait ServerRoom extends Room {

  private var clients: Seq[Client] = Seq.empty
  this.onCreate()

  /**
   * Add a client to the room. Triggers the onJoin
   *
   * @param client the client to add
   */
  def addClient(client: Client): Unit = {
    this.clients = client +: this.clients
    this.onJoin(client)
  }

  /**
   * Remove a client from the room. Triggers onLeave
   *
   * @param client the client that leaved
   */
  def removeClient(client: Client): Unit = {
    this.clients = this.clients.filter(_.id != client.id)
    this.onLeave(client)
  }

  /**
   *
   * @param client the client to check
   * @return true if the client is authorized to perform actions on this room
   */
  def clientAuthorized(client: Client): Boolean = {
    this.connectedClients.contains(client)
  }

  /**
   * @return the list of connected clients
   */
  def connectedClients: Seq[Client] = this.clients

  /**
   * Send a message to a single client
   *
   * @param client  the client that will receive the message
   * @param message the message to send
   * @tparam M the type of the message
   */
  def tell[M](client: Client, message: M): Unit = this.clients.filter(_.id == client.id).foreach(_.send(message))

  /**
   * Broadcast a message to all clients connected
   *
   * @param message the message to send
   * @tparam M he type of the message
   */
  def broadcast[M](message: M): Unit = this.clients.foreach(_.send(message))

  /**
   * Close this room
   */
  def close(): Unit =
    this.onClose()

  //TODO: what to do here?


  /**
   * Called as soon as the room is created by the server
   */
  def onCreate(): Unit

  /**
   * Called as soon as the room is closed
   */
  def onClose(): Unit

  /**
   * Called when a user joins the room
   *
   * @param client tha user that joined
   */
  def onJoin(client: Client): Unit

  /**
   * Called when a user left the room
   *
   * @param client the client that left
   */
  def onLeave(client: Client): Unit

  /**
   * Called when the room receives a message
   *
   * @param client the client that sent the message
   * @param message the message received
   * @tparam M the type of the message
   */
  def onMessageReceived[M](client: Client, message: M)


}

object ServerRoom {

  /**
   * Creates a simple server room with an empty behavior.
   *
   * @param id the id of the room
   * @return the room
   */
  def apply(id: String): ServerRoom = new BasicServerRoom(id)
}

private class BasicServerRoom(override val roomId: String) extends ServerRoom {
  override def onCreate(): Unit = {}

  override def onClose(): Unit = {}

  override def onJoin(client: Client): Unit = {}

  override def onLeave(client: Client): Unit = {}

  override def onMessageReceived[M](client: Client, message: M): Unit = {}
}
