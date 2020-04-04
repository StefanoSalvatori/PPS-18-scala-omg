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

  protected val DEFAULT_BUFFER_SIZE: Int = 128
  protected val DEFAULT_OVERFLOW_STRATEGY: OverflowStrategy = OverflowStrategy.dropHead


  def createFlow(overflowStrategy: OverflowStrategy = DEFAULT_OVERFLOW_STRATEGY,
                 bufferSize: Int = DEFAULT_BUFFER_SIZE)
                (implicit materializer: Materializer): Flow[Message, Message, Any]

}
