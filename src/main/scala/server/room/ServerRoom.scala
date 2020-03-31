package server.room

import akka.actor.{Actor, ActorRef}

object ServerRoom {

  trait RoomStrategy {

    def onCreate()

    def onJoin()

    def onMessageReceived()

    def onLeave()

  }

  object RoomStrategy{

    def empty: RoomStrategy = new RoomStrategy {
      override def onCreate() = {}

      override def onJoin() = {}

      override def onMessageReceived() = {}

      override def onLeave() = {}
    }
  }


}

