package server.utils

import server.room.{Client, RoomState, ServerRoom}

object ExampleRooms {

  case class ClosableRoomWithState() extends ServerRoom with RoomState[String] {
    override def onCreate(): Unit = {
      this.startStateUpdate()
    }
    override def onClose(): Unit = {}
    override def onJoin(client: Client): Unit = {}
    override def onLeave(client: Client): Unit = {}
    override def onMessageReceived(client: Client, message: Any): Unit = {
      message.toString match {
        case "close" => this.close()
        case "ping" => this.tell(client, "pong")
      }
    }
    override def currentState: String = "game state"
    override def joinConstraints: Boolean = true
  }

  val roomWithStateType = "roomWithState"

  //________________________________________________

  case class NoPropertyRoom() extends ServerRoom {

    override def onCreate(): Unit = { }
    override def onClose(): Unit = { }
    override def onJoin(client: Client): Unit = { }
    override def onLeave(client: Client): Unit = { }
    override def onMessageReceived(client: Client, message: Any): Unit = { }
    override def joinConstraints: Boolean = { true }
  }

  val noPropertyRoomType = "noProperty"


  //________________________________________________

  case class MyRoom() extends ServerRoom {

    val a: Int = 0
    val b: String = "abc"

    override def onCreate(): Unit = { }
    override def onClose(): Unit = { }
    override def onJoin(client: server.room.Client): Unit = { }
    override def onLeave(client: server.room.Client): Unit = { }
    override def onMessageReceived(client: server.room.Client, message: Any): Unit = { }
    override def joinConstraints: Boolean = { true }
  }

  val myRoomType = "myRoom"
}
