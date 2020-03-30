package server

import akka.Done
import akka.actor.PoisonPill
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import common.actors.ApplicationActorSystem
import server.ServerActor._
import server.room.RoomStrategy
import server.route_service.RouteService

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

trait GameServer {

  /**
   * The host where the game server runs.
   */
  val host: String

  /**
   * The port the server is listening on.
   */
  val port: Int

  /**
   * Start the server listening at [[server.GameServer#host]]:[[server.GameServer#port]].
   *
   * @return A future that completes when the server is started
   */
  def start(): Future[Unit]

  /**
   * Shutdown the server.
   *
   * @return A future that completes when the server is terminated
   */
  def shutdown(): Future[Unit]

  /**
   * Define what to do on server start up
   *
   * @param callback the function to call on startup
   */
  def onStart(callback: => Unit)

  /**
   * Define what to do on server shutdown
   *
   * @param callback the function to call on shutdown
   */
  def onShutdown(callback: => Unit)


  /**
   * Adds a new type of room to the server
   *
   * @param roomTypeName The name of the room
   * @param roomStrategy The strategy that the room will use
   */
  def defineRoom(roomTypeName: String, roomStrategy: RoomStrategy)

}


object GameServer {

  val SERVER_TERMINATION_DEADLINE: FiniteDuration = 2 seconds


  /**
   * Create a new game server at the the specified host and port.
   * <br>
   * You can pass optional routes that will be used by the server so that, for example, you can use this both as game
   * server and web-server.
   *
   * @note <b>If your routes contain 'rooms' as base path they will not be used because that is a reserved path used
   *       internally</b>
   * @param host           the hostname of the server
   * @param port           the port it will be listening on
   * @param existingRoutes (optional) additional routes that will be used by the server
   * @return an instance if a [[server.GameServer]]
   */
  def apply(host: String, port: Int, existingRoutes: Route = reject): GameServer = new GameServerImpl(host, port, existingRoutes)


}

/**
 * Implementation of a game server. It uses Akka Http internally with a private actor system to handle the server.
 *
 * @param host             the address of the server
 * @param port             the port it will listen on
 * @param additionalRoutes (optional) additional routes that will be used by the server
 **/
private class GameServerImpl(override val host: String,
                             override val port: Int,
                             private val additionalRoutes: Route = reject) extends GameServer
  with LazyLogging
  with ApplicationActorSystem {

  import GameServer._
  import akka.pattern.ask

  implicit private val actorRequestsTimeout: Timeout = Timeout(5 seconds)
  private val serverActor = actorSystem actorOf ServerActor(SERVER_TERMINATION_DEADLINE)


  private val routeService = RouteService()

  private var onStart: () => Unit = () => {}
  private var onShutdown: () => Unit = () => {}

  override def onStart(callback: => Unit): Unit = this.onStart = () => callback

  override def onShutdown(callback: => Unit): Unit = this.onShutdown = () => callback

  override def start(): Future[Unit] = {
    (serverActor ? StartServer(host, port, routeService.route ~ additionalRoutes))
      .asInstanceOf[Future[ServerResponse]] flatMap {
      case Started  =>
        this.onStart()
        Future.successful()
      case ErrorResponse(msg) => Future.failed(new IllegalStateException(msg))
      case _ => Future.failed(new IllegalStateException())
    }
  }

  override def shutdown(): Future[Unit] = {
    (serverActor ? StopServer).asInstanceOf[Future[ServerResponse]] flatMap  {
      case Stopped =>
        this.onShutdown()
        Future.successful()
      case ErrorResponse(msg) => Future.failed(new IllegalStateException(msg))
      case _ => Future.failed(new IllegalStateException())
    }
  }

  override def defineRoom(roomTypeName: String, room: RoomStrategy) : Unit = {
    routeService.addRouteForRoomType(roomTypeName, room)
  }

}


