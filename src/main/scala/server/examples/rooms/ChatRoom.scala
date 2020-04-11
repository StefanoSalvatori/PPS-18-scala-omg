package server.examples.rooms

import server.room.{Client, ServerRoom}

case class ChatRoom() extends ServerRoom {

  override def onCreate(): Unit = println("Room Created")

  override def onClose(): Unit = println("Room Closed")

  override def onJoin(client: Client): Unit = this.broadcast(s"${client.id} Connected")

  override def onLeave(client: Client): Unit = this.broadcast(s"${client.id} Leaved")

  override def onMessageReceived(client: Client, message: Any): Unit = this.broadcast(s"${client.id}: $message")

  override def joinConstraints: Boolean = true
}
