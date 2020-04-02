package common.actors

import akka.actor.{ActorSystem, Terminated}
import scala.concurrent.{ExecutionContextExecutor, Future}

trait ApplicationActorSystem {

  implicit lazy val actorSystem: ActorSystem = ActorSystem("Application")
  implicit lazy val executionContext: ExecutionContextExecutor = actorSystem.dispatcher

  def terminateActorSystem(): Future[Terminated] = this.actorSystem.terminate()
}
