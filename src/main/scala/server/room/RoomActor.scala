package server.room

import akka.actor.{Actor, PoisonPill, Props, Timers}
import common.communication.CommunicationProtocol.ProtocolMessageType._
import common.communication.CommunicationProtocol.SessionId.SessionId
import common.communication.CommunicationProtocol.ProtocolMessage
import common.room.Room.RoomPassword
import server.core.RoomHandler

import scala.concurrent.ExecutionContextExecutor

private[server] object RoomActor {

  /**
   * It creates a room actor associated to a room.
   * @param serverRoom the room associated to the actor
   * @param roomHandler the room handler that is charged to handle the room
   * @return [[akka.actor.Props]] needed to create the actor
   */
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

  // Command
  sealed trait RoomCommand
  case class Join(client: Client, sessionId: SessionId, password: RoomPassword) extends RoomCommand
  case class Leave(client: Client) extends RoomCommand
  case class Reconnect(client: Client, sessionId: SessionId, password: RoomPassword) extends RoomCommand
  case class Msg(client: Client, payload: Any) extends RoomCommand
  case object Close extends RoomCommand

  case object StartAutoCloseTimeout

  private trait InternalMessage
  private case object AutoCloseRoom extends InternalMessage

  private case object AutoCloseRoomTimer
}

/**
 * This actor acts as a wrapper for a server room to handle concurrency.
 *
 * @param serverRoom the room linked with this actor
 */
private[server] class RoomActor(private val serverRoom: ServerRoom,
                private val roomHandler: RoomHandler) extends Actor with Timers {

  import RoomActor._

  implicit val executionContext: ExecutionContextExecutor = this.context.system.dispatcher

  serverRoom setRoomActor self

  override def preStart(): Unit = {
    super.preStart()
    this.serverRoom.onCreate()
    if (serverRoom isAutoCloseAllowed) {
      self ! StartAutoCloseTimeout
    }
  }

  override def postStop(): Unit = {
    super.postStop()
  }

  override def receive: Receive = {
    case Join(client, _, password) => // a client joins for the first time
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
      if (serverRoom isAutoCloseAllowed) {
        self ! StartAutoCloseTimeout
      }

    case Msg(client, payload) =>
      if (serverRoom isClientAuthorized client) {
        this.serverRoom.onMessageReceived(client, payload)
      } else {
        client send ClientNotAuthorized
        sender ! ProtocolMessage(ClientNotAuthorized, client.id)
      }

    case Close =>
      roomHandler removeRoom serverRoom.roomId
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
