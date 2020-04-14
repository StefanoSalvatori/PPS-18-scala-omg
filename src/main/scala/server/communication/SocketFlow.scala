package server.communication

import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Flow
import akka.stream.{Materializer, OverflowStrategy}
import common.communication.SocketSerializer

/**
 * Trait that allow to create a socket flow to handle socket input and output
 *
 */
trait SocketFlow {

  protected val DefaultBufferSize: Int = 128
  protected val DefaultOverflowStrategy: OverflowStrategy = OverflowStrategy.dropHead


  def createFlow(overflowStrategy: OverflowStrategy = DefaultOverflowStrategy,
                 bufferSize: Int = DefaultBufferSize): Flow[Message, Message, Any]

}
