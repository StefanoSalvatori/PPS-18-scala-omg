package server

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Stash, Status}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Sink

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.{FiniteDuration, _}
import scala.language.postfixOps

object ServerActor {

  private val DEFAULT_DEADLINE: FiniteDuration = 3 seconds

  sealed trait ServerEvent

  sealed trait Command extends ServerEvent
  case class StartServer(host: String, port: Int, routes: Route) extends Command
  case object StopServer extends Command

  private sealed trait InternalMessage extends ServerEvent
  private case class ServerStarted(binding: Http.ServerBinding) extends InternalMessage
  private case object ServerStopped extends InternalMessage

  sealed trait ServerResponse
  case object Started extends ServerResponse
  case object Stopped extends ServerResponse
  case class Failure(exception: Throwable) extends ServerResponse

  case class Error(msg: String) extends ServerResponse
  object ServerAlreadyRunning extends Error("Server already running")
  object ServerIsStarting extends Error("Server is starting")
  object ServerAlreadyStopped extends Error("Server already stopped")
  object ServerIsStopping extends Error("Server is stopping")

  def apply(terminationDeadline: FiniteDuration = DEFAULT_DEADLINE): Props = Props(classOf[ServerActor], terminationDeadline)
}

class ServerActor(private val terminationDeadline: FiniteDuration) extends Actor with ActorLogging with Stash {

  import server.ServerActor._

  implicit val actorSystem: ActorSystem = context.system
  implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher

  override def receive: Receive = {
    case StartServer(host, port, routes) =>
      val source = Http(this.actorSystem).bind(host, port)
      val serverStartedFuture = source.to(Sink.foreach(_ handleWith routes)).run()

      import akka.pattern.pipe
      serverStartedFuture map (result => ServerStarted(result)) pipeTo self
      context.become(serverStarting(sender))

    case StopServer => sender ! ServerAlreadyStopped
  }

  def serverStarting(replyTo: ActorRef): Receive = {
    case ServerStarted(binding) =>
      replyTo ! Started
      context.become(serverRunning(binding))
      unstashAll()
    case _: StartServer => sender ! ServerIsStarting
    case StopServer => stash()
    case Status.Failure(exception: Exception) =>
      replyTo ! ServerActor.Failure(exception)
      context.become(receive)

  }

  def serverRunning(binding: Http.ServerBinding): Receive = {
    case _: StartServer =>
      sender ! ServerAlreadyRunning
    case StopServer =>
      import akka.pattern.pipe
      binding.terminate(this.terminationDeadline)
      binding.whenTerminated
        .flatMap(_ => Http(this.actorSystem).shutdownAllConnectionPools())
        .map(_ => ServerStopped) pipeTo self
      context.become(serverStopping(binding, sender))
  }

  def serverStopping(binding: Http.ServerBinding, replyTo: ActorRef): Receive = {
    case ServerStopped =>
      replyTo ! Stopped
      context.become(receive)
      unstashAll()
    case StopServer => sender ! ServerIsStopping
    case _: StartServer => stash()
    case Status.Failure(exception: Exception) =>
      replyTo ! ServerActor.Failure(exception)
      context.become(serverRunning(binding))
  }
}
