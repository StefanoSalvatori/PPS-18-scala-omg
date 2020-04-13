package server.examples.rooms

import server.room.{Client, GameLoop, ServerRoom, SynchronizedRoomState}

object ExampleRooms {

  case class MyRoom() extends ServerRoom {

    val a: Int = 0
    val b: String = "abc"

    override def onCreate(): Unit = {}
    override def onClose(): Unit = {}
    override def onJoin(client: Client): Unit = {}
    override def onLeave(client: Client): Unit = {}
    override def onMessageReceived(client: Client, message: Any): Unit = {}
    override def joinConstraints: Boolean = true
  }

  case class RoomWithGameLoopAndSync() extends ServerRoom with GameLoop with SynchronizedRoomState[Integer] {

    private var count = 0

    // Server room
    override def onCreate(): Unit = {
      this.startWorldUpdate()
      this.startStateUpdate()
    }
    override def onClose(): Unit = {}
    override def onJoin(client: Client): Unit = {}
    override def onLeave(client: Client): Unit = {}
    override def onMessageReceived(client: Client, message: Any): Unit = {}
    override def joinConstraints: Boolean = true

    // Game loop

    override def updateWorld(): Unit = {
      count = count + 1
      println(count)
    }

    override def currentState: Integer = {
      println("UPDATE " + count)
      count
    }
  }
}
