package scalaomg.server.examples.rooms

import scalaomg.server.room.{Client, GameLoop, RoomPropertyMarker, ServerRoom, SynchronizedRoomState}

object ExampleRooms {

  case class MyRoom() extends ServerRoom {

    @RoomPropertyMarker val a: Int = 0
    @RoomPropertyMarker val b: String = "abc"

    override def onCreate(): Unit = {}

    override def onClose(): Unit = {}

    override def onJoin(client: Client): Unit = {}

    override def onLeave(client: Client): Unit = {}

    override def onMessageReceived(client: Client, message: Any): Unit = {}

    override def joinConstraints: Boolean = true
  }

  //noinspection ScalaUnusedSymbol
  case class RoomWithGameLoopAndSync() extends ServerRoom with GameLoop with SynchronizedRoomState[Integer] {

    @RoomPropertyMarker private val maxClients = 2
    private var count = 0

    override val stateUpdateRate: Int = 100
    override val worldUpdateRate: Int = 100

    // Server room
    override def onCreate(): Unit = {
      this.startWorldUpdate()
      this.startStateSynchronization()
    }

    override def onClose(): Unit = {}

    override def onJoin(client: Client): Unit = {}

    override def onLeave(client: Client): Unit = {}

    override def onMessageReceived(client: Client, message: Any): Unit = {}

    override def joinConstraints: Boolean = true

    override def updateWorld(elapsed: Long): Unit = {
      count = count + 1
      if (count % 10 == 0) {
        println(count)
        System.out.flush()
      }
    }

    override def currentState: Integer = {
      if (count % 10 == 0) {
        println("UPDATE " + count)
        System.out.flush()
      }
      count
    }
  }
}
