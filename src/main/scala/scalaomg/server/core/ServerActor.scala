package scalaomg.server.core

import akka.actor.{Actor, ActorRef, ActorSystem, Props, Stash, Status}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.pipe
import akka.stream.scaladsl.Sink
import scalaomg.common.room.Room.RoomType
import scalaomg.common.room.RoomProperty
import scalaomg.server.core.ServerActor._
import scalaomg.server.matchmaking.{Matchmaker, MatchmakingHandler}
import scalaomg.server.room.ServerRoom
import scalaomg.server.routing_service.RoutingService

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.{FiniteDuration, _}
import scala.language.postfixOps

private[server] object ServerActor {

  sealed trait ServerEvent

  case class CreateRoom(roomType: RoomType, properties: Set[RoomProperty] = Set.empty)

  // Command
  sealed trait Command extends ServerEvent
  case class StartServer(host: String, port: Int) extends Command
  case object StopServer extends Command
  case class AddRoute(routeName: String, room: () => ServerRoom) extends Command
  case class AddRouteForMatchmaking[T](routeName: String,
                                       room: () => ServerRoom,
                                       matchmaker: Matchmaker[T]) extends Command

  // Server response
  sealed trait ServerResponse
  case object Started extends ServerResponse
  case object Stopped extends ServerResponse
  case object RouteAdded extends ServerResponse
  case object RoomCreated extends ServerResponse
  case class ServerFailure(exception: Throwable) extends ServerResponse

  // State error
  case class StateError(msg: String) extends ServerResponse
  object ServerAlreadyRunning extends StateError("Server already running")
  object ServerIsStarting extends StateError("Server is starting")
  object ServerAlreadyStopped extends StateError("Server already stopped")
  object ServerIsStopping extends StateError("Server is stopping")

  // Internal message
  private trait InternalMessage extends ServerEvent
  private case class ServerStarted(binding: Http.ServerBinding) extends InternalMessage
  private case object ServerStopped extends InternalMessage

  private val DefaultDeadline: FiniteDuration = 3 seconds

  def apply(terminationDeadline: FiniteDuration = DefaultDeadline, additionalRoutes: Route): Props =
    Props(classOf[ServerActor], terminationDeadline, additionalRoutes)
}

private class ServerActor(private val terminationDeadline: FiniteDuration,
                  private val additionalRoutes: Route) extends Actor with Stash {
  implicit val actorSystem: ActorSystem = context.system
  implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher

  private val roomHandler = RoomHandler()
  private val matchmakingHandler = MatchmakingHandler(roomHandler)
  private val routeService = RoutingService(roomHandler, matchmakingHandler)

  override def receive: Receive = idle orElse roomHandling

  def idle: Receive = {
    case StartServer(host, port) =>
      val source = Http().bind(host, port)
      val serverStartedFuture = source.to(Sink.foreach(_ handleWith (routeService.route ~ additionalRoutes))).run()
      serverStartedFuture map (result => ServerStarted(result)) pipeTo self
      context.become(serverStarting(sender) orElse roomHandling)
    case StopServer =>
      sender ! ServerAlreadyStopped
  }

  def serverStarting(replyTo: ActorRef): Receive = {
    case ServerStarted(binding) =>
      replyTo ! Started
      context.become(serverRunning(binding) orElse roomHandling)
      unstashAll()
    case StartServer(_, _) => sender ! ServerIsStarting
    case StopServer => stash()
    case Status.Failure(exception: Exception) =>
      replyTo ! ServerActor.ServerFailure(exception)
      context.become(receive)
  }

  def serverRunning(binding: Http.ServerBinding): Receive = {
    case StartServer(_, _) =>
      sender ! ServerAlreadyRunning

    case StopServer =>
      binding.terminate(this.terminationDeadline)
      binding.whenTerminated
        .flatMap(_ => Http(this.actorSystem).shutdownAllConnectionPools())
        .map(_ => ServerStopped) pipeTo self
      context.become(serverStopping(binding, sender) orElse roomHandling)
  }

  def serverStopping(binding: Http.ServerBinding, replyTo: ActorRef): Receive = {
    case ServerStopped =>
      replyTo ! Stopped
      context.become(receive)
      unstashAll()

    case StopServer => sender ! ServerIsStopping

    case StartServer(_, _) => stash()

    case Status.Failure(exception: Exception) =>
      replyTo ! ServerActor.ServerFailure(exception)
      context.become(serverRunning(binding) orElse roomHandling)
  }

  private def roomHandling: Receive = {
    case AddRoute(roomType, room) =>
      this.routeService.addRouteForRoomType(roomType, room)
      sender ! RouteAdded

    case AddRouteForMatchmaking(roomType, room, matchmaker) =>
      this.routeService.addRouteForMatchmaking(roomType, room)(matchmaker)
      sender ! RouteAdded

    case CreateRoom(roomType, properties) =>
      this.roomHandler.createRoom(roomType, properties)
      sender ! RoomCreated
  }
}
