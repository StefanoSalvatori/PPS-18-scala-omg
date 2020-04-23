package client.matchmaking

import akka.actor.{ActorRef, Stash}
import client.matchmaking.MatchmakingActor.{JoinMatchmaking, LeaveMatchmaking}
import client.utils.MessageDictionary.{HttpMatchmakingSocketRequest, HttpSocketFail, HttpSocketSuccess, SocketError}
import client.{BasicActor, HttpClient}
import common.communication.BinaryProtocolSerializer
import common.communication.CommunicationProtocol.{ProtocolMessage, SessionId}
import common.communication.CommunicationProtocol.ProtocolMessageType._
import common.room.Room.RoomType

import scala.util.{Failure, Success}

/**
 * Handles the connection with the matchmaker .
 * Notify the client when the room is created
 */
sealed trait MatchmakingActor extends BasicActor

object MatchmakingActor {

  case class JoinMatchmaking()

  case class LeaveMatchmaking()

  import akka.actor.Props

  def apply(roomType: RoomType, httpServerUri: String, clientInfo: Any with java.io.Serializable): Props =
    Props(classOf[MatchmakingActorImpl], roomType, httpServerUri, clientInfo)
}


class MatchmakingActorImpl(private val roomType: RoomType,
                           private val httpServerUri: String,
                           private val clientInfo: Any with java.io.Serializable) extends MatchmakingActor with Stash {
  private val httpClient = context.system actorOf HttpClient(httpServerUri)

  override def receive: Receive = onReceive orElse fallbackReceive

  def waitSocketResponse(replyTo: ActorRef): Receive =
    onWaitSocketResponse(replyTo) orElse
      handleErrors(replyTo) orElse
      fallbackReceive

  def socketOpened(outRef: ActorRef, replyTo: ActorRef): Receive =
    onSocketOpened(outRef, replyTo) orElse
      heartbeatResponse(outRef) orElse
      handleErrors(replyTo) orElse
      fallbackReceive


  def onReceive: Receive = {
    case JoinMatchmaking =>
      httpClient ! HttpMatchmakingSocketRequest(this.roomType, BinaryProtocolSerializer())
      context.become(waitSocketResponse(sender))
  }

  def onWaitSocketResponse(replyTo: ActorRef): Receive = {
    case HttpSocketFail(code) =>
      replyTo ! Failure(new Exception(code.toString))
      context.become(receive)

    case HttpSocketSuccess(outRef) =>
      outRef ! ProtocolMessage(JoinQueue,  payload = clientInfo)
      context.become(socketOpened(outRef, replyTo))
      unstashAll()

    case LeaveMatchmaking => stash()
  }

  def onSocketOpened(outRef: ActorRef, replyTo: ActorRef): Receive = {
    case ProtocolMessage(MatchCreated, _, ticket) =>
      replyTo ! Success(ticket)

    case LeaveMatchmaking =>
      outRef ! ProtocolMessage(LeaveQueue)
      sender ! Success
  }

  private def handleErrors(replyTo: ActorRef): Receive = {
    case SocketError(_) => //ignore
  }

  /**
   * Keep the socket alive
   */
  private def heartbeatResponse(roomSocket: ActorRef): Receive = {
    case ProtocolMessage(Ping, _, _) => roomSocket ! ProtocolMessage(Pong)

  }

}