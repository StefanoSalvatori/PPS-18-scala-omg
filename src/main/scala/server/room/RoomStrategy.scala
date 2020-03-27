package server.room

trait RoomStrategy {

  def onJoin()

  def onMessageReceived()

  def onLeave()

  def onCreate()

}
