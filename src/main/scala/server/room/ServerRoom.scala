package server.room

import akka.actor.{Actor, ActorRef}

object ServerRoom {
  type RoomId = String
  type RoomType = String

  trait RoomStrategy {

    def onCreate()

    def onJoin()

    def onMessageReceived()

    def onLeave()

  }


}

