package client.utils

import akka.actor.{ActorRef, PoisonPill}
import client.utils.MessageDictionary.{HttpSocketRequest, SocketError}
import common.communication.CommunicationProtocol.ProtocolMessage
import common.communication.CommunicationProtocol.ProtocolMessageType.{Ping, Pong}
import common.communication.SocketSerializer

/**
 *
 */
private[client] trait SocketService extends BasicActor {


  def makeSocketRequest[T](parser: SocketSerializer[T], route: String, uri: String): Unit = {
    val httpClient = context.system actorOf HttpService(uri)
    httpClient ! HttpSocketRequest(parser, route)
    //httpClient ! PoisonPill
  }

  def handleErrors(f: Throwable => Unit): Receive = {
    case SocketError(throwable) => f(throwable)
  }

  def heartbeatResponse(outRef: ActorRef): Receive = {
    case ProtocolMessage(Ping, _, _) => outRef ! ProtocolMessage(Pong)
  }
}

