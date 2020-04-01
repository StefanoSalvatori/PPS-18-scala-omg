package client

import java.util.concurrent.TimeUnit

import akka.pattern.ask
import MessageDictionary._
import common.actors.ApplicationActorSystem
import common.{Room, Routes}

import scala.concurrent.Await

sealed trait Client {
  def createPublicRoom(roomType: String): Unit
  def joinedRooms: Set[Room]
  def shutdown(): Unit
}

object Client {
  def apply(serverAddress: String, serverPort: Int): ClientImpl = new ClientImpl(serverAddress, serverPort)
}

class ClientImpl(private val serverAddress: String, private val serverPort: Int) extends Client with ApplicationActorSystem {

  private val requestTimeout = 5 // Seconds
  import akka.util.Timeout
  implicit val timeout: Timeout = Timeout(requestTimeout, TimeUnit.SECONDS)

  private val serverUri = Routes.uri(serverAddress, serverPort)

  private val coreClient = actorSystem actorOf CoreClient(serverUri)

  override def createPublicRoom(roomType: String): Unit = coreClient ! CreatePublicRoom(roomType)

  override def joinedRooms: Set[Room] =
    Await.result(coreClient ? GetJoinedRooms, timeout.duration).asInstanceOf[JoinedRooms].rooms

  override def shutdown(): Unit = super.terminateActorSystem()
}
