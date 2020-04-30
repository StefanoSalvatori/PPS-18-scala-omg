package client.matchmaking

import akka.actor.{ActorRef, Props, Stash}
import client.utils.MessageDictionary._
import client.utils.{SocketActor, SocketFailException}
import common.communication.BinaryProtocolSerializer
import common.communication.CommunicationProtocol.ProtocolMessageType._
import common.communication.CommunicationProtocol.{ProtocolMessage, SocketSerializable}
import common.http.Routes
import common.room.Room.RoomType

import scala.util.{Failure, Success}

/**
 * Handles the connection with the serverside matchmaker .
 * Notify the client when the match is created
 */
private[client] sealed trait MatchmakingActor extends SocketActor[ProtocolMessage]

private[client] object MatchmakingActor {
  def apply(roomType: RoomType, httpServerUri: String, clientInfo: SocketSerializable): Props =
    Props(classOf[MatchmakingActorImpl], roomType, httpServerUri, clientInfo)
}


class MatchmakingActorImpl(private val roomType: RoomType,
                           private val httpServerUri: String,
                           private val clientInfo: SocketSerializable)
  extends MatchmakingActor with Stash  {

  override val uri = httpServerUri
  override val serializer = BinaryProtocolSerializer()

  override def receive: Receive = onReceive orElse fallbackReceive

  def waitSocketResponse(replyTo: ActorRef): Receive =
    onWaitSocketResponse(replyTo) orElse
      handleErrors(_ => {}) orElse
      fallbackReceive

  def socketOpened(outRef: ActorRef, replyTo: ActorRef): Receive =
    onSocketOpened(outRef, replyTo) orElse
      heartbeatResponse(outRef) orElse
      handleErrors(_ => {}) orElse
      fallbackReceive


  def onReceive: Receive = {
    case JoinMatchmaking =>
      makeSocketRequest(Routes.matchmakingSocketConnection(this.roomType))
      context.become(waitSocketResponse(sender))
  }

  def onWaitSocketResponse(replyTo: ActorRef): Receive = {
    case HttpSocketFail(code) =>
      replyTo ! Failure(SocketFailException(code.toString))
      context.become(receive)
      unstashAll()

    case HttpSocketSuccess(outRef) =>
      outRef ! ProtocolMessage(JoinQueue, payload = clientInfo)
      context.become(socketOpened(outRef, replyTo))
      unstashAll()

    case LeaveMatchmaking => stash()
  }

  def onSocketOpened(outRef: ActorRef, replyTo: ActorRef): Receive = {
    case ProtocolMessage(MatchCreated, _, ticket) =>  replyTo ! Success(ticket)

    case LeaveMatchmaking =>
      outRef ! ProtocolMessage(LeaveQueue)
      sender ! Success
  }


}