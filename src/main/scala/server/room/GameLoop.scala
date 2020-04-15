package server.room

import common.room.RoomProperty
import server.room.RoomActor.WorldUpdateTick
import server.utils.Timer

/**
 * It defines a room that uses game loop, i.e. the state of the world is updated periodically
 */
trait GameLoop { self: ServerRoom =>

  private val worldTimer: Timer = Timer.withExecutor()

  /**
   * How often the world will be updated (time expressed in milliseconds)
   */
  protected val worldUpdateRate = 50 //milliseconds

  /**
   * Start updating the world with a fixed period
   */
  def startWorldUpdate(): Unit =
    worldTimer.scheduleAtFixedRate(() => this.generateWorldUpdateTick(), 0, worldUpdateRate)

  /**
   * Stop updating the world
   */
  def stopWorldUpdate(): Unit = worldTimer.stopTimer()

  /**
   * Function called at each tick to update the world
   */
  def updateWorld(): Unit

  private def generateWorldUpdateTick(): Unit = {
    self.roomActor ! WorldUpdateTick()
  }
}

object GameLoop {

  private case class BasicServerRoomWithGameLoop() extends ServerRoom with GameLoop {
    override def onCreate(): Unit = { }
    override def onClose(): Unit = { }
    override def onJoin(client: Client): Unit = { }
    override def onLeave(client: Client): Unit = { }
    override def onMessageReceived(client: Client, message: Any): Unit = { }
    override def joinConstraints: Boolean = true
    override def updateWorld(): Unit = { }
  }

  private def apply(): BasicServerRoomWithGameLoop = BasicServerRoomWithGameLoop()

  /**
   * Getter of the game loop properties
   *
   * @return a set containing the defined properties
   */
  def defaultProperties: Set[RoomProperty] = ServerRoom propertyDifferenceFrom GameLoop()
}

