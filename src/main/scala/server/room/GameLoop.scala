package server.room

import server.room.RoomActor.WorldUpdateTick
import server.utils.Timer

/**
 * It defines a room that uses game loop, i.e. the state of the world is updated periodically
 */
trait GameLoop { self: ServerRoom =>

  private val worldTimer: Timer = new Timer{ }

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

