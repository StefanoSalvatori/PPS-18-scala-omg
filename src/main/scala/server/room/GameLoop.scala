package server.room

import server.room.RoomActor.WorldUpdateTick
import server.utils.{Timer, Timer2}

/**
 * It defines a room that uses game loop, i.e. the state of the world is updated periodically
 */
trait GameLoop extends Timer2 { self: ServerRoom =>

  /**
   * How often the wolrd will be updated (time expressed in milliseconds)
   */
  protected val worldUpdateRate = 50 //milliseconds

  /**
   * Start updating the world with a fixed period
   */
  def startWorldUpdate(): Unit =
    this.scheduleAtFixedRate2(() => this.generateWorldUpdateTick(), 0, worldUpdateRate)

  /**
   * Stop updating the world
   */
  def stopWorldUpdate(): Unit = this.stopTimer2()

  /**
   * Function called at each tick to update the world
   */
  def updateWorld(): Unit

  private def generateWorldUpdateTick(): Unit = {
    self.roomActor ! WorldUpdateTick()
  }
}

