package server.examples.rooms

import server.room.{Client, ServerRoom}

object ExampleRooms {

  case class MyRoom() extends ServerRoom {

    val a: Int = 0
    val b: String = "abc"

    override def onCreate(): Unit = {}
    override def onClose(): Unit = {}
    override def onJoin(client: Client): Unit = {}
    override def onLeave(client: Client): Unit = {}
    override def onMessageReceived(client: Client, message: Any): Unit = {}
  }
}
