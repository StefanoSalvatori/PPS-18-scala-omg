package server

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Stash}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Sink

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.language.postfixOps
import scala.concurrent.duration._

object ServerActor {

  private val DEFAULT_DEADLINE: FiniteDuration = 3 seconds

  sealed trait ServerRequest
  case class StartServer(host: String, port: Int, routes: Route) extends ServerRequest
  case object StopServer extends ServerRequest


  sealed trait ServerReponse
  case object Started extends ServerReponse
  case object Stopped extends ServerReponse

  case class ErrorResponse(msg: String) extends ServerReponse
  object ServerAlreadyRunning extends ErrorResponse("Server already running")
  object ServerIsStarting extends ErrorResponse("Server is starting")
  object ServerAlreadyStopped extends ErrorResponse("Server already stopped")
  object ServerIsStopping extends ErrorResponse("Server is stopping")


  //messages used internally by the actor
  private case class ServerStarted(binding: Http.ServerBinding, sender: ActorRef)
  private case class ServerStopped(sender: ActorRef)


  def apply(terminationDeadline: FiniteDuration = DEFAULT_DEADLINE): Props = Props(classOf[ServerActor], terminationDeadline)
}

class ServerActor(private val terminationDeadline: FiniteDuration) extends Actor with ActorLogging with Stash {

  import server.ServerActor._

  implicit val actorSystem: ActorSystem = context.system
  implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher

  override def receive: Receive = {
    case StartServer(host, port, routes) =>
      val source = Http().bind(host, port)
      val serverStartedFuture = source.to(Sink.foreach(_ handleWith routes)).run()

      import akka.pattern.pipe
      val requestSender = sender
      serverStartedFuture map (result => ServerStarted(result, requestSender)) pipeTo self
      context.become(serverStarting(serverStartedFuture))

    case StopServer => sender ! ServerAlreadyStopped
  }

  def serverStarting(started: Future[Http.ServerBinding]): Receive = {
    case ServerStarted(binding, requestSender) =>
      requestSender ! Started
      context.become(serverRunning(binding))
      unstashAll()
    case _: StartServer => sender ! ServerIsStarting
    case StopServer => stash()
  }

  def serverRunning(binding: Http.ServerBinding): Receive = {
    case _: StartServer =>
      sender ! ServerAlreadyRunning
    case StopServer =>
      import akka.pattern.pipe
      val requestSender = sender
      binding.terminate(this.terminationDeadline) map (_ => ServerStopped(requestSender)) pipeTo self
      context.become(serverStopping())
  }

  def serverStopping(): Receive = {
    case ServerStopped(requestSender) =>
      requestSender ! Stopped
      context.become(receive)
      unstashAll()
    case StopServer => sender ! ServerIsStopping
    case _: StartServer => stash()

  }
}
