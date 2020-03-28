package server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.LazyLogging
import server.room.RoomStrategy
import server.route_service.RouteService

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

sealed trait ServerEvent

case class ServerStarted(host: String, port: Int) extends ServerEvent

case class ServerTerminated(value: Any) extends ServerEvent

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
   *
   * @return true if the server is started
   */
  def isStarted: Boolean

  /**
   * Start the server listening at [[server.GameServer#host]]:[[server.GameServer#port]].
   *
   * @return A future that completes when the server is started
   */
  def start(): Future[ServerStarted]

  /**
   * Shutdown the server.
   *
   * @return A future that completes when the server is terminated
   */
  def shutdown(): Future[ServerTerminated]

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
   *
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
 * @param host the address of the server
 * @param port the port it will listen on
 * @param additionalRoutes (optional) additional routes that will be used by the server
 **/
private class GameServerImpl(override val host: String,
                     override val port: Int,
                     private val additionalRoutes: Route = reject) extends GameServer with LazyLogging {

  implicit var system: ActorSystem = _
  implicit var executionContext: ExecutionContext = _


  private val routeService = RouteService()
  private var serverBinding: Option[Http.ServerBinding] = Option.empty

  private var onStart: () => Unit = () => {}
  private var onShutdown: () => Unit = () => {}

  override def onStart(callback: => Unit): Unit = this.onStart = () => callback

  override def onShutdown(callback: => Unit): Unit = this.onShutdown = () => callback

  override def isStarted: Boolean = this.serverBinding.nonEmpty

  override def start(): Future[ServerStarted] = {
    this.serverBinding match {
      case Some(_) => Future.failed(new IllegalStateException("Server already started"))
      case None => this.startServer()
    }
  }

  override def shutdown(): Future[ServerTerminated] = {
    this.serverBinding match {
      case Some(_) => this.shutdownServer()
      case None => Future.failed(new IllegalStateException("Can't shutdown server if it is not started"))
    }
  }

  override def defineRoom(roomTypeName: String, room: RoomStrategy): Unit = {
    routeService.addRouteForRoomType(roomTypeName, room)
  }

  private def onClientConnected(connection: Http.IncomingConnection): Unit = {
    connection handleWith routeService.route ~ additionalRoutes
  }

  /**
   * Unbind http socket and terminate actorsystem
   */
  private def shutdownServer(): Future[ServerTerminated] = {
    for {
      _ <- this.serverBinding.get.terminate(GameServer.SERVER_TERMINATION_DEADLINE)
      _ <- this.system.terminate()
    } yield {
      this.onShutdown()
      this.serverBinding = Option.empty
      ServerTerminated()
    }
  }

  private def startServer(): Future[ServerStarted] = {
    this.setupActorSystem()
    val serverSource = Http().bind(this.host, this.port)
    val serverStartedFuture = serverSource.to(Sink.foreach(this.onClientConnected)).run()
    serverStartedFuture map (binding => {
      this.onStart()
      this.serverBinding = Option(binding)
      ServerStarted(this.host, this.port)
    })

  }

  private def setupActorSystem(): Unit = {
      this.system = ActorSystem()
      this.executionContext = this.system.dispatcher
  }


}


