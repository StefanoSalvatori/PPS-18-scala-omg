package server.utils

import server.communication.ConnectionConfigurations
import server.room.{Client, GameLoop, RoomPropertyMarker, ServerRoom, SynchronizedRoomState}

/**
 * Rooms used for testing purpose.
 */
object ExampleRooms {

  case class RoomWithState() extends ServerRoom with SynchronizedRoomState[Integer] {
    private var internalState = RoomWithState.RoomInitialState
    override val stateUpdateRate: Int = RoomWithState.UpdateRate

    override def onCreate(): Unit = {}

    override def onClose(): Unit = this.stopStateSynchronization()

    override def onJoin(client: Client): Unit = {}

    override def onLeave(client: Client): Unit = {}

    override def onMessageReceived(client: Client, message: Any): Unit = {}

    override def currentState: Integer = this.internalState


    //Only used for testing
    def changeState(newState: Int): Unit = this.internalState = newState
  }

  object RoomWithState {
    val UpdateRate = 100 //milliseconds
    val RoomInitialState: Int = 0
  }

  val roomWithStateType = "roomWithState"

  //_________________________________________________

  case class RoomWithGameLoop() extends ServerRoom with GameLoop {

    private var count = RoomWithGameLoop.initialState
    override val worldUpdateRate: Int = RoomWithGameLoop.updateRate

    override def joinConstraints: Boolean = true

    override def onCreate(): Unit = {}

    override def onClose(): Unit = {}

    override def onJoin(client: Client): Unit = {}

    override def onLeave(client: Client): Unit = {}

    override def onMessageReceived(client: Client, message: Any): Unit = {}

    override def updateWorld(elapsed: Long): Unit = {
      count = count + 1
      receivedTicks = receivedTicks + 1
    }

    // Used for testing purpose
    def state: Int = count

    var receivedTicks: Int = 0
  }

  object RoomWithGameLoop {
    val initialState = 0
    val updateRate = 100 // millis
  }

  val roomWithGameLoopType = "roomWithGameLoop"

  //__________________________________________________

  case class RoomWithReconnection() extends ServerRoom {
    private val ReconnectionTime = 10 //s
    override def onCreate(): Unit = {}

    override def onClose(): Unit = {}

    override def onJoin(client: Client): Unit = {}

    override def onLeave(client: Client): Unit = {
      this.allowReconnection(client, ReconnectionTime)
    }

    override def onMessageReceived(client: Client, message: Any): Unit = {}

    override def joinConstraints: Boolean = true
  }

  val roomWithReconnection = "roomWithReconnection"

  //________________________________________________

  object ClosableRoomWithState {
    val ChangeStateMessage = "changeState"
    val CloseRoomMessage = "close"
    val PingMessage = "ping"
    val PongResponse = "pong"
  }

  case class ClosableRoomWithState() extends ServerRoom with SynchronizedRoomState[String] {
    import ClosableRoomWithState._
    import scala.concurrent.duration._
    override val stateUpdateRate = 200
    override val socketConfigurations = ConnectionConfigurations(2 seconds)
    private var gameState = "gameState"

    override def onCreate(): Unit = this.startStateSynchronization()

    override def onClose(): Unit = {}

    override def onJoin(client: Client): Unit = {}

    override def onLeave(client: Client): Unit = {}

    override def onMessageReceived(client: Client, message: Any): Unit = {
      message.toString match {
        case CloseRoomMessage => this.close()
        case PingMessage => this.tell(client, PongResponse)
        case ChangeStateMessage => this.gameState = "gameState_updated"
      }
    }

    override def currentState: String = this.gameState

    override def joinConstraints: Boolean = true

  }

  val closableRoomWithStateType = "closableRoomWithState"

  //________________________________________________

  case class NoPropertyRoom() extends ServerRoom {

    override def onCreate(): Unit = {}

    override def onClose(): Unit = {}

    override def onJoin(client: Client): Unit = {}

    override def onLeave(client: Client): Unit = {}

    override def onMessageReceived(client: Client, message: Any): Unit = {}

    override def joinConstraints: Boolean = {
      true
    }
  }

  val noPropertyRoomType = "noProperty"

  //________________________________________________

  case class RoomWithProperty() extends ServerRoom {

    @RoomPropertyMarker private val a: Int = 0
    @RoomPropertyMarker private val b: String = "abc"
    private val c: Int = 0

    override def onCreate(): Unit = {}

    override def onClose(): Unit = {}

    override def onJoin(client: server.room.Client): Unit = {}

    override def onLeave(client: server.room.Client): Unit = {}

    override def onMessageReceived(client: server.room.Client, message: Any): Unit = {}

    override def joinConstraints: Boolean = {
      true
    }
  }

  val roomWithPropertyType = "roomWithProperty"

  // ____________________________________________________________________

  case class RoomWithProperty2() extends ServerRoom {

    @RoomPropertyMarker private var a: Int = 1
    @RoomPropertyMarker private var b: String = "a"
    @RoomPropertyMarker private var c: Boolean = true
    private var d: Int = 0

    override def onCreate(): Unit = {}

    override def onClose(): Unit = {}

    override def onJoin(client: Client): Unit = {}

    override def onLeave(client: Client): Unit = {}

    override def onMessageReceived(client: Client, message: Any): Unit = {}

    override def joinConstraints: Boolean = true
  }

  val roomWithProperty2Type = "roomWithProperty2"
}
