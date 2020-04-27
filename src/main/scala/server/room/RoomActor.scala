package server.room

import akka.actor.{Actor, ActorLogging, PoisonPill, Props, Timers}
import common.communication.CommunicationProtocol.ProtocolMessageType._
import common.communication.CommunicationProtocol.SessionId.SessionId
import common.communication.CommunicationProtocol.{ProtocolMessage, SessionId}
import common.room.Room.RoomPassword
import server.RoomHandler

import scala.concurrent.ExecutionContextExecutor

object RoomActor {

  sealed trait RoomCommand

  case class Join(client: Client, sessionId: SessionId, password: RoomPassword) extends RoomCommand

  case class Leave(client: Client) extends RoomCommand

  case class Reconnect(client: Client, sessionId: SessionId, password: RoomPassword) extends RoomCommand

  case class Msg(client: Client, payload: Any) extends RoomCommand

  case object Close extends RoomCommand

  case object StartAutoCloseTimeout

  private trait InternalMessage

  private case object AutoCloseRoomTimer

  private case object AutoCloseRoom extends InternalMessage

  def apply(serverRoom: ServerRoom, roomHandler: RoomHandler): Props = Props(classOf[RoomActor], serverRoom, roomHandler)

  /**
   * It triggers the synchronization of public room state between clients.
   *
   * @param onTick A consumer that specifies how to sync a given client
   */
  case class StateSyncTick(onTick: Client => Unit)

  /**
   * It triggers the update of the room state.
   */
  case class WorldUpdateTick(lastUpdate: Long)

}

/**
 * This actor acts as a wrapper for a server room to handle concurrency
 *
 * @param serverRoom the room linked with this actor
 */
class RoomActor(private val serverRoom: ServerRoom,
                private val roomHandler: RoomHandler) extends Actor with ActorLogging with Timers {

  import RoomActor._

  implicit val executionContext: ExecutionContextExecutor = this.context.system.dispatcher

  serverRoom setRoomActor self

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
    case Join(client, _, password) => //client first join
      this.timers cancel AutoCloseRoomTimer
      val joined = serverRoom tryAddClient(client, password)
      sender ! (if (joined) ProtocolMessage(JoinOk, client.id) else ProtocolMessage(ClientNotAuthorized, client.id))

    case Reconnect(client, _, _) =>
      this.timers cancel AutoCloseRoomTimer
      val reconnected = serverRoom tryReconnectClient client
      sender ! (if (reconnected) ProtocolMessage(JoinOk, client.id) else ProtocolMessage(ClientNotAuthorized, client.id))

    case Leave(client) =>
      this.serverRoom removeClient client
      sender ! ProtocolMessage(LeaveOk)

    case Msg(client, payload) =>
      if (this.serverRoom.clientAuthorized(client)) {
        this.serverRoom.onMessageReceived(client, payload)
      } else {
        client.send(ClientNotAuthorized)
        sender ! ProtocolMessage(ClientNotAuthorized, client.id)
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

    case WorldUpdateTick(0) =>
      serverRoom.asInstanceOf[GameLoop].updateWorld(0)

    case WorldUpdateTick(lastUpdate) =>
      serverRoom.asInstanceOf[GameLoop].updateWorld(System.currentTimeMillis() - lastUpdate)
  }
}
