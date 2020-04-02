package server.room

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object RoomActor {
  sealed trait RoomCommand
  // case class Tell[T](client: Client, msg: T) extends RoomCommand
  // case class NotifyAll[T](msg : T) extends RoomCommand
  case class Join(client: Client) extends RoomCommand
  case class Leave(client: Client) extends RoomCommand
  case class Msg[T](client: Client, payload: T) extends RoomCommand

  sealed trait RoomResponse
  case object JoinOk extends RoomResponse
  case object ClientLeaved extends RoomResponse
  case object ClientNotAuthorized extends RoomResponse

  def apply(serverRoom: ServerRoom): Props = Props(classOf[RoomActor], serverRoom)

}


class RoomActor(private val serverRoom: ServerRoom) extends Actor with ActorLogging {

  import RoomActor._


  override def preStart(): Unit = {
    super.preStart()
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
    case Msg(client, payload) => this.serverRoom.onMessageReceived(client, payload)
  }


}
