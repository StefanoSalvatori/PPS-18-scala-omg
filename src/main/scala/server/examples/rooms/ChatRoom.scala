package server.examples.rooms

import server.room.{Client, ServerRoom}

class ChatRoom(override val roomId: String) extends ServerRoom {
  override def onCreate(): Unit = println("Room Created")

  override def onClose(): Unit = println("Room Closed")

  override def onJoin(client: Client): Unit = this.broadcast(s"${client.id} Connected")

  override def onLeave(client: Client): Unit = this.broadcast(s"${client.id} Leaved")

  override def onMessageReceived[M](client: Client, message: M): Unit = this.broadcast(s"${client.id}: $message")
}
