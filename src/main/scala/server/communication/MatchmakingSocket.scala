package server.communication

import akka.actor.ActorRef
import common.communication.CommunicationProtocol.{ProtocolMessage, ProtocolMessageSerializer}

import scala.concurrent.duration._
import common.communication.CommunicationProtocol.ProtocolMessageType
import server.matchmaking.MatchmakingService.{LeaveQueue, _}

import scala.concurrent.duration.Duration

case class MatchmakingSocket(private val matchmakingService: ActorRef,
                             override val parser: ProtocolMessageSerializer) extends Socket[ProtocolMessage] {
  override protected val pingMessage: ProtocolMessage = ProtocolMessage(ProtocolMessageType.Ping)
  override protected val pongMessage: ProtocolMessage = ProtocolMessage(ProtocolMessageType.Pong)

  //no idle timeout since we don't know how much it will take to match a player with other ones. We only check that
  // client is still active with an heartbeat every 5 seconds
  override val connectionConfig: ConnectionConfigurations = ConnectionConfigurations(Duration.Inf, 5 seconds)

  override protected val onMessageFromSocket: PartialFunction[ProtocolMessage, Unit] = {
    case ProtocolMessage(ProtocolMessageType.JoinQueue, _, clientInfo) =>
      matchmakingService ! JoinQueue(this.client, clientInfo)
    case ProtocolMessage(ProtocolMessageType.LeaveQueue, _, _) =>
      matchmakingService ! LeaveQueue(this.client)
  }

  override protected def onSocketClosed(): Unit = matchmakingService ! LeaveQueue(this.client)
}
