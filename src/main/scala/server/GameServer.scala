package server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.LazyLogging
import server.room.{Room, RoomOptions}
import server.route_service.RouteService

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.language.postfixOps

sealed trait ServerEvent

case class ServerStarted(host: String, port: Int) extends ServerEvent

case class ServerTerminated(value: Any) extends ServerEvent

trait GameServer {

  type Room = String

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
   * Add a new type of room to the server
   *
   * @param room The type of room to add
   */
  def defineRoom(room: Room)

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
  def apply(host: String, port: Int)(implicit system: ActorSystem): GameServer = new GameServerImpl(host, port, system)


  /*def fromExistingServer(host: String, port: Int)(implicit system: ActorSystem): GameServer =
    new GameServerImpl("", 0, system)*/
}





private class GameServerImpl(override val host: String,
                             override val port: Int,
                             implicit val system: ActorSystem) extends GameServer with LazyLogging {


  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  private var serverBinding: Option[Http.ServerBinding] = Option.empty

  private val routeService: RouteService = RouteService()

  override def start(): Future[ServerStarted] = {
    val serverSource = Http().bind(this.host, this.port)
    val serverStartedFuture = serverSource.to(Sink.foreach(this.onClientConnected)).run()
    serverStartedFuture.transform(binding => {
      this.serverBinding = Option(binding)
      ServerStarted(this.host, this.port)
    }, identity)
  }

  override def shutdown(): Future[ServerTerminated] = {
    this.serverBinding match {
      case Some(binding) =>
        binding.terminate(GameServer.SERVER_TERMINATION_DEADLINE).transform(_ => {
          this.serverBinding = Option.empty
          ServerTerminated()
        }, identity)
      case None => Future.failed(new IllegalStateException("Can't shutdown server if it is not started"))
    }
  }

  override def defineRoom(room: Room): Unit = {
    logger.debug("Defined " + room)
  }

  private def onClientConnected(connection: Http.IncomingConnection): Unit = {
    logger.debug("GAMESERVER: Accepted new connection from " + connection.remoteAddress)
    connection handleWith routeService.route
  }

  /**
   *
   * @return true if the server is started
   */
  override def isStarted: Boolean = this.serverBinding.nonEmpty
}


