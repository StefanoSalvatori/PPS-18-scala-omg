package server.utils

import common.room.SharedRoom.RoomId
import server.room.{Client, ServerRoom}

object ExampleRooms {

  case class NoPropertyRoom() extends ServerRoom {

    override def onCreate(): Unit = { }
    override def onClose(): Unit = { }
    override def onJoin(client: Client): Unit = { }
    override def onLeave(client: Client): Unit = { }
    override def onMessageReceived(client: Client, message: Any): Unit = { }
  }
  val noPropertyRoomType = "noProperty"

  case class MyRoom() extends ServerRoom {

    val a: Int = 0
    val b: String = "abc"

    override def onCreate(): Unit = { }
    override def onClose(): Unit = { }
    override def onJoin(client: server.room.Client): Unit = { }
    override def onLeave(client: server.room.Client): Unit = { }
    override def onMessageReceived(client: server.room.Client, message: Any): Unit = { }
  }
  val myRoomType = "myRoom"
}
