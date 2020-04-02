package server.room

import common.CommonRoom.Room


trait ServerRoom extends Room {

  private var clients: Seq[Client] = Seq.empty
  this.onCreate()

  def addClient(client: Client): Unit = {
    this.clients = client +: this.clients
    this.onJoin(client)
  }

  def removeClient(client: Client): Unit = {
    this.clients = this.clients.filter(_.id != client.id)
    this.onLeave(client)
  }

  def connectedClients: Seq[Client] = this.clients

  def tell[M](client: Client, message: M): Unit = this.clients.filter(_.id == client.id).foreach(_.send(message))

  def broadcast[M](message: M): Unit = this.clients.foreach(_.send(message))

  def close(): Unit =
    this.onClose()
  //TODO: what to do here?


  def onCreate(): Unit

  def onClose(): Unit

  def onJoin(client: Client): Unit

  def onLeave(client: Client): Unit

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
