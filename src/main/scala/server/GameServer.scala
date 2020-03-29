package server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.LazyLogging
import server.room.ServerRoom.RoomStrategy
import server.route_service.RouteService

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
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
   * @param callback the function to call on startup
   */
  def onStart(callback: => Unit)

  /**
   * Define what to do on server shutdown
   * @param callback the function to call on shutdown
   */
  def onShutdown(callback: => Unit)

  /**
   * Adds a new type of room to the server
   * @param roomTypeName The name of the room
   * @param roomStrategy The strategy that the room will use
   */
  def defineRoom(roomTypeName: String, roomStrategy: RoomStrategy)

}


object GameServer {

  val SERVER_TERMINATION_DEADLINE: FiniteDuration = 2 seconds

  /**
   * Create a new game server at the the specified host and port
   *
   * @param host the hostname of the server
   * @param port the port it will be listening on
   * @return an instance if a [[server.GameServer]]
   */
  def apply(host: String, port: Int): GameServer = new GameServerImpl(host, port)


  /*def fromExistingServer(host: String, port: Int)(implicit system: ActorSystem): GameServer =
    new GameServerImpl("", 0, system)*/
}

/**
 * Implementation of a game server. It uses Akka Http internally with a private actor system to handle the server.
 * @param host the address of the server
 * @param port the port it will listen on
 */
private class GameServerImpl(override val host: String,
                             override val port: Int) extends GameServer  with LazyLogging {


  /**
   * Internally creates actor system
   */
  implicit private var system: ActorSystem = _
  implicit private var executionContext: ExecutionContextExecutor = _

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
    logger.debug("Defined " + roomTypeName)
    routeService.addRouteForRoomType(roomTypeName, room)
  }

  /**
   * Unbind http socket and terminate actorsystem
   */
  private def shutdownServer(): Future[ServerTerminated] = {
    for {
      _ <- this.serverBinding.get.terminate(GameServer.SERVER_TERMINATION_DEADLINE)
      _ <- system.terminate()
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

  private def onClientConnected(connection: Http.IncomingConnection): Unit = {
    logger.debug("GAMESERVER: Accepted new connection from " + connection.remoteAddress)
    connection handleWith routeService.route
  }

  /**
   * Create a new actorsystem and restart the execution context
   */
  private def setupActorSystem(): Unit = {
    this.system = ActorSystem()
    this.executionContext = system.dispatcher
  }

}


