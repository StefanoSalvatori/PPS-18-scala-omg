package common.actors

import akka.actor.ActorSystem

import scala.concurrent.ExecutionContextExecutor

trait ApplicationActorSystem {

  implicit lazy val actorSystem: ActorSystem = ActorSystem()
  implicit lazy val executionContext: ExecutionContextExecutor = actorSystem.dispatcher


  def terminateActorSystem(): Unit = {
    this.actorSystem.terminate()
  }


}
