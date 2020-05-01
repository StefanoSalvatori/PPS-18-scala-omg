package client.utils

import akka.actor.ActorRef
import client.utils.MessageDictionary.{HttpSocketRequest, SocketError}
import common.communication.CommunicationProtocol.ProtocolMessage
import common.communication.CommunicationProtocol.ProtocolMessageType.{Ping, Pong}
import common.communication.SocketSerializer


/**
 * An actor that handles a web socket communication
 */
private[client] trait SocketActor[T] extends BasicActor {

  val uri: String
  val serializer: SocketSerializer[T]

  private lazy val httpClient = context.system actorOf HttpService(uri)

  def makeSocketRequest(route: String): Unit = {
    httpClient ! HttpSocketRequest(serializer, route)
  }

  def handleErrors(f: Throwable => Unit): Receive = {
    case SocketError(throwable) => f(throwable)
  }


  def heartbeatResponse(outRef: ActorRef): Receive = {
    case ProtocolMessage(Ping, _, _) => outRef ! ProtocolMessage(Pong)
  }
}

