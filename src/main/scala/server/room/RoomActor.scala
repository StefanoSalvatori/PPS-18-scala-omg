package server.room

import akka.actor.{Actor, ActorLogging, Props}
import common.communication.CommunicationProtocol.ProtocolMessageType._

object RoomActor {
  sealed trait RoomCommand
  // case class Tell[T](client: Client, msg: T) extends RoomCommand
  // case class NotifyAll[T](msg : T) extends RoomCommand
  case class Join(client: Client) extends RoomCommand
  case class Leave(client: Client) extends RoomCommand
  case class Msg(client: Client, payload: Any) extends RoomCommand

  sealed trait RoomResponse
  case object ClientLeaved extends RoomResponse

  def apply(serverRoom: ServerRoom): Props = Props(classOf[RoomActor], serverRoom)

}

/**
 * This actor acts as a wrapper for server rooms to handle concurrency
 *
 * @param serverRoom the room linked with this actor
 */
class RoomActor(private val serverRoom: ServerRoom) extends Actor with ActorLogging {

  import RoomActor._


  override def preStart(): Unit = {
    super.preStart()
    // this.serverRoom.onCreate()
  }

  override def postStop(): Unit = {
    super.postStop()
    this.serverRoom.close()
  }

  override def receive: Receive = {
    // case Tell(client, msg) => this.serverRoom.tell(client, msg)
    // case NotifyAll(msg) => this.serverRoom.broadcast(msg)
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
  }


}
