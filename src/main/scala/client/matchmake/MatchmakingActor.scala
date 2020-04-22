package client.matchmake

import akka.actor.ActorRef
import client.matchmake.MatchmakingActor.{JoinMatchmake, LeaveMatchmake}
import client.utils.MessageDictionary.{HttpMatchmakingSocketRequest, HttpSocketFail, HttpSocketSuccess, SocketError}
import client.{BasicActor, HttpClient}
import common.communication.BinaryProtocolSerializer
import common.communication.CommunicationProtocol.ProtocolMessage
import common.communication.CommunicationProtocol.ProtocolMessageType.{JoinQueue, LeaveQueue, MatchCreated, Ping, Pong}
import common.room.Room.RoomType

import scala.util.{Failure, Success}

/**
 * Handles the connection with the matchmaker .
 * Notify the client when the room is created
 */
sealed trait MatchmakingActor extends BasicActor

object MatchmakingActor {

  case class JoinMatchmake()

  case class LeaveMatchmake()

  import akka.actor.Props

  def apply(roomType: RoomType, httpServerUri: String): Props =
    Props(classOf[MatchmakingActorImpl], roomType, httpServerUri)
}


class MatchmakingActorImpl(private val roomType: RoomType,
                           private val httpServerUri: String) extends MatchmakingActor {
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
    case JoinMatchmake =>
      httpClient ! HttpMatchmakingSocketRequest(this.roomType, BinaryProtocolSerializer())
      context.become(waitSocketResponse(sender))
  }

  def onWaitSocketResponse(replyTo: ActorRef): Receive = {
    case HttpSocketFail(code) =>
      replyTo ! Failure(new Exception(code.toString))
      context.become(receive)

    case HttpSocketSuccess(outRef) =>
      outRef ! ProtocolMessage(JoinQueue)
      context.become(socketOpened(outRef, replyTo))
  }

  def onSocketOpened(outRef: ActorRef, replyTo: ActorRef): Receive = {
    case ProtocolMessage(MatchCreated, clientId, roomId) =>
      replyTo ! Success((clientId, roomId))
    case LeaveMatchmake => outRef ! ProtocolMessage(LeaveQueue)
  }

  private def handleErrors(replyTo: ActorRef): Receive = {
    case SocketError(ex) =>
      replyTo ! Failure(new Exception(ex.toString))
      logger.debug("error " + ex.toString)

  }

  /**
   * Keep the socket alive
   */
  private def heartbeatResponse(roomSocket: ActorRef): Receive = {
    case ProtocolMessage(Ping, _, _) => roomSocket ! ProtocolMessage(Pong)
  }

}