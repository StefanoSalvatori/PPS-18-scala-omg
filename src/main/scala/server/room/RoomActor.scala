package server.room

import akka.actor.{Actor, ActorLogging, PoisonPill, Props, Timers}
import common.communication.CommunicationProtocol.ProtocolMessageType._
import common.communication.CommunicationProtocol.RoomProtocolMessage
import common.room.Room.RoomPassword
import server.RoomHandler

import scala.concurrent.ExecutionContextExecutor

object RoomActor {
  sealed trait RoomCommand
  case class Join(client: Client, sessionId: String, password: RoomPassword) extends RoomCommand
  case class Leave(client: Client) extends RoomCommand
  case class Msg(client: Client, payload: Any) extends RoomCommand
  case object Close extends RoomCommand

  case object StartAutoCloseTimeout


  private trait InternalMessage
  private case object AutoCloseRoomTimer
  private case object AutoCloseRoom extends InternalMessage

  def apply(serverRoom: ServerRoom, roomHandler: RoomHandler): Props = Props(classOf[RoomActor], serverRoom, roomHandler)

  // Timers
  case class StateSyncTick(f: Client => Unit)
  case class WorldUpdateTick()
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

  implicit val executionContext: ExecutionContextExecutor = this.context.system.dispatcher

  serverRoom setAssociatedActor self

  override def preStart(): Unit = {
    super.preStart()
    this.serverRoom.onCreate()
    if (this.serverRoom.checkAutoClose()) {
      self ! StartAutoCloseTimeout
    }
  }

  override def postStop(): Unit = {
    super.postStop()
  }

  override def receive: Receive = {
    case Join(client, sessionId, password) =>
      this.timers.cancel(AutoCloseRoomTimer)
      if (sessionId.isEmpty) {
        val joined = serverRoom tryAddClient(client, password)
        sender ! (if (joined) RoomProtocolMessage(JoinOk, client.id) else RoomProtocolMessage(ClientNotAuthorized, client.id))
      } else { //if sessionId not empty means reconnection
        val reconnected = serverRoom tryReconnectClient client
        sender ! (if (reconnected) RoomProtocolMessage(JoinOk, client.id) else RoomProtocolMessage(ClientNotAuthorized, client.id))
      }

    case Leave(client) =>
      this.serverRoom.removeClient(client)
      sender ! RoomProtocolMessage(LeaveOk)

    case Msg(client, payload) =>
      if (this.serverRoom.clientAuthorized(client)) {
        this.serverRoom.onMessageReceived(client, payload)
      } else {
        client.send(ClientNotAuthorized)
        sender ! RoomProtocolMessage(ClientNotAuthorized, client.id)
      }

    case Close =>
      this.roomHandler.removeRoom(this.serverRoom.roomId)
      self ! PoisonPill

    case StartAutoCloseTimeout =>
      this.timers.startSingleTimer(AutoCloseRoomTimer, AutoCloseRoom, this.serverRoom.autoCloseTimeout)

    case AutoCloseRoom =>
      this.serverRoom.close()

    case StateSyncTick(onTick) =>
      serverRoom.connectedClients foreach onTick

    case WorldUpdateTick() =>
      serverRoom.asInstanceOf[GameLoop].updateWorld()
  }
}
