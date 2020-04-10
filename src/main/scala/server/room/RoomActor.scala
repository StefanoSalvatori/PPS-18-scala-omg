package server.room

import akka.actor
import akka.actor.{Actor, ActorLogging, PoisonPill, Props, Scheduler, Timers}
import common.communication.CommunicationProtocol.ProtocolMessageType._
import server.RoomHandler

import scala.concurrent.ExecutionContextExecutor

object RoomActor {
  sealed trait RoomCommand
  case class Join(client: Client) extends RoomCommand
  case class Leave(client: Client) extends RoomCommand
  case class Msg(client: Client, payload: Any) extends RoomCommand

  sealed trait RoomResponse
  case object ClientLeaved extends RoomResponse

  private trait InternalMessage
  private case object CheckRoomStateTimer
  private case object CheckRoomState extends InternalMessage

  def apply(serverRoom: ServerRoom, roomHandler: RoomHandler): Props = Props(classOf[RoomActor], serverRoom, roomHandler)

}

/**
 * This actor acts as a wrapper for server rooms to handle concurrency
 *
 * @param serverRoom the room linked with this actor
 */
class RoomActor(private val serverRoom: ServerRoom,
                private val roomHandler: RoomHandler) extends Actor with ActorLogging with Timers {

  import RoomActor._
  import scala.concurrent.duration._

  implicit val CheckRoomStateRate: FiniteDuration = 50 millis
  implicit val executionContext: ExecutionContextExecutor = this.context.system.dispatcher

  override def preStart(): Unit = {
    this.timers.startTimerAtFixedRate(CheckRoomStateTimer, CheckRoomState, CheckRoomStateRate)
    super.preStart()
    this.serverRoom.onCreate()
  }

  override def postStop(): Unit = {
    super.postStop()
  }

  override def receive: Receive = {
    case Join(client) =>
      this.serverRoom.addClient(client) //TODO: add checks if user can join
      sender ! JoinOk
    case Leave(client) =>
      this.serverRoom.removeClient(client)
      sender ! ClientLeaved
    case Msg(client, payload) =>
      if (this.serverRoom.clientAuthorized(client)) {
        this.serverRoom.onMessageReceived(client, payload)
      } else {
        client.send(ClientNotAuthorized)
        sender ! ClientNotAuthorized
      }
    case CheckRoomState =>
      if (this.serverRoom.isClosed) {
        this.roomHandler.removeRoom(this.serverRoom.roomId)
        self ! PoisonPill
      }
  }

}
