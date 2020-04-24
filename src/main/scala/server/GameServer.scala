package server

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import common.room.RoomProperty
import server.matchmaking.Matchmaker
import server.room.ServerRoom

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
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
   * Stops the server. It can be restarted calling start
   *
   * @return A future that completes when the server is terminated
   */
  def stop(): Future[Unit]

  /**
   * Terminate the game server permanently
   */
  def terminate(): Future[Unit]

  /**
   * Define what to do on server start up
   *
   * @param callback the function to call on startup
   */
  def onStart(callback: => Unit)

  /**
   * Define what to do on server stop
   *
   * @param callback the function to call on shutdown
   */
  def onStop(callback: => Unit)

  /**
   *
   * @param roomFactory the function to create the room given the room id
   */
  def defineRoom(roomTypeName: String, roomFactory: () => ServerRoom)

  /**
   * Define a type of room that enable matchmaking functions
   */
  def defineRoomWithMatchmaking(roomTypeName: String, roomFactory: () => ServerRoom,
                                matchmaker: Matchmaker)


  /**
   * Creates a room of a given type. The type should be defined before calling this method
   *
   * @param roomType the type of room to create
   */
  def createRoom(roomType: String, properties: Set[RoomProperty] = Set.empty): Unit
}


object GameServer {
  /**
   * Timeout for the graceful shutdown of the server
   */
  val ServerTerminationDeadline: FiniteDuration = 2 seconds


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
  def apply(host: String, port: Int, existingRoutes: Route = reject): GameServer =
    new GameServerImpl(host, port, existingRoutes)
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
  with LazyLogging {

  import GameServer._
  import server.ServerActor._
  import akka.pattern.ask

  private implicit val ActorRequestTimeout: Timeout = 10 seconds

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher

  private val serverActor = actorSystem actorOf ServerActor(ServerTerminationDeadline, additionalRoutes)
  private var onStart: () => Unit = () => {}
  private var onShutdown: () => Unit = () => {}


  override def start(): Future[Unit] = {
    (serverActor ? StartServer(host, port))
      .asInstanceOf[Future[ServerResponse]]
      .flatMap {
        case Started => this.onStart(); Future.successful()
        case StateError(msg) => Future.failed(new IllegalStateException(msg))
        case ServerFailure(exception) => Future.failed(exception)
        case _ => Future.failed(new IllegalStateException())
      }
  }

  override def stop(): Future[Unit] = {
    (serverActor ? StopServer)
      .asInstanceOf[Future[ServerResponse]]
      .flatMap {
        case Stopped => this.onShutdown(); Future.successful()
        case StateError(msg) => Future.failed(new IllegalStateException(msg))
        case ServerFailure(exception) => Future.failed(exception)
        case _ => Future.failed(new IllegalStateException())
      }
  }

  override def onStart(callback: => Unit): Unit = this.onStart = () => callback

  override def onStop(callback: => Unit): Unit = this.onShutdown = () => callback

  override def defineRoom(roomTypeName: String, roomFactory: () => ServerRoom): Unit =
    Await.ready(serverActor ? AddRoute(roomTypeName, roomFactory), ActorRequestTimeout.duration)

  override def defineRoomWithMatchmaking(roomTypeName: String,
                                         roomFactory: () => ServerRoom,
                                         matchmaker: Matchmaker): Unit =
    Await.ready(serverActor ? AddRouteForMatchmaking(roomTypeName, roomFactory, matchmaker), ActorRequestTimeout.duration)


  override def createRoom(roomType: String, properties: Set[RoomProperty] = Set.empty): Unit =
    Await.ready(serverActor ? CreateRoom(roomType, properties), ActorRequestTimeout.duration)

  override def terminate(): Future[Unit] =
    for {
      _ <- this.stop()
      _ <- this.actorSystem.terminate()
    } yield {}
}




