package server

import server.room.{Client, ServerRoom}

object RoomProva {
  def apply(roomId: String): RoomProva = new RoomProva(roomId)
}

class RoomProva(override val roomId: String) extends ServerRoom {

  val a = 3
  val b = "abc"
  val c = false

  override def toString: String = {s"Room Prova: $a $b $c"}

  override def onCreate(): Unit = println("Room Created")
  override def onClose(): Unit = println("Room Closed")
  override def onJoin(client: Client): Unit = this.broadcast(s"${client.id} Connected")
  override def onLeave(client: Client): Unit = this.broadcast(s"${client.id} Leaved")
  override def onMessageReceived[M](client: Client, message: M): Unit = this.broadcast(s"${client.id}: $message")
}
