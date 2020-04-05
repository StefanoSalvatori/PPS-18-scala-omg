package common.communication

import akka.http.scaladsl.model.ws.Message

import scala.util.Try

/**
 * A trait that defines how to read and write messages through a socket
 *
 * @tparam T the type of message to write and read
 */
trait SocketSerializer[T] {

  /**
   * Parse an incoming message from a socket
   *
   * @param msg the msg to parse
   * @return the corresponding [[common.communication.CommunicationProtocol.RoomProtocolMessage]]
   */
  def parseFromSocket(msg: Message): Try[T]


  /**
   * Write a socket message in the socket
   *
   * @param msg the protocol message to write
   * @return the corresponding socket message
   */
  def writeToSocket(msg: T): Message

}


