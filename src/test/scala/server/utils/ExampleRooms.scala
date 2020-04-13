package server.utils

import server.room.{Client, SynchronizedRoomState, ServerRoom}

/**
 * Rooms used for testing purpose.
 */
object ExampleRooms {

  case class RoomWithState() extends ServerRoom with SynchronizedRoomState[Integer] {
    private var internalState = RoomWithState.RoomInitialState
    override val updateRate: Int = RoomWithState.UpdateRate

    override def onCreate(): Unit = {}
    override def onClose(): Unit = this.stopStateUpdate()
    override def onJoin(client: Client): Unit = {}
    override def onLeave(client: Client): Unit = {}
    override def onMessageReceived(client: Client, message: Any): Unit = {}
    override def currentState: Integer = this.internalState
    override def joinConstraints: Boolean = { true }

    //Only used for testing
    def changeState(newState: Int): Unit = this.internalState = newState
  }

  object RoomWithState {
    val UpdateRate = 100 //milliseconds
    val RoomInitialState: Int = 0
  }

  val roomWithStateType = "roomWithState"

  //__________________________________________________

  case class ClosableRoomWithState() extends ServerRoom with SynchronizedRoomState[String] {
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

  val closableRoomWithStateType = "closableRoomWithState"

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
