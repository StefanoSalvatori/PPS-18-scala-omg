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

  sealed trait RoomResponse
  case object ClientLeaved extends RoomResponse

  private trait InternalMessage
  private case object CheckRoomStateTimer
  private case object CheckRoomState extends InternalMessage

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
  implicit val CheckRoomStateRate: FiniteDuration = 50 millis
  implicit val executionContext: ExecutionContextExecutor = this.context.system.dispatcher

  serverRoom setAssociatedActor self

  override def preStart(): Unit = {
    this.timers.startTimerAtFixedRate(CheckRoomStateTimer, CheckRoomState, CheckRoomStateRate)
    super.preStart()
    this.serverRoom.onCreate()
  }

  override def postStop(): Unit = {
    super.postStop()
  }

  override def receive: Receive = {
    case Join(client, sessionId, password) =>
      serverRoom.synchronized {
        if (sessionId.isEmpty) {
          val joined = serverRoom tryAddClient(client, password)
          sender ! (if (joined) RoomProtocolMessage(JoinOk, client.id) else RoomProtocolMessage(ClientNotAuthorized, client.id))
        } else { //if sessionId not empty means reconnection
          val reconnected = serverRoom tryReconnectClient (client)
          sender ! (if (reconnected) RoomProtocolMessage(JoinOk, client.id) else RoomProtocolMessage(ClientNotAuthorized, client.id))
        }
      }
    case Leave(client) =>
      serverRoom.synchronized {
        this.serverRoom.removeClient(client)
        sender ! ClientLeaved
      }
    case Msg(client, payload) =>
      serverRoom.synchronized {
        if (this.serverRoom.clientAuthorized(client)) {
          this.serverRoom.onMessageReceived(client, payload)
        } else {
          client.send(ClientNotAuthorized)
          sender ! RoomProtocolMessage(ClientNotAuthorized, client.id)
        }
      }

    case CheckRoomState =>
      serverRoom.synchronized {
        if (this.serverRoom.isClosed) {
          this.roomHandler.removeRoom(this.serverRoom.roomId)
          self ! PoisonPill
        }
      }

    case StateSyncTick(f) =>
      serverRoom.connectedClients foreach f

    case WorldUpdateTick() =>
      serverRoom.asInstanceOf[GameLoop].updateWorld()
  }

}
