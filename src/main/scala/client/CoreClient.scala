package client

import client.room.ClientRoom.ClientRoom

sealed trait CoreClient extends BasicActor

object CoreClient {
  import akka.actor.Props
  def apply(serverUri: String): Props = Props(classOf[CoreClientImpl], serverUri)
}

class CoreClientImpl(private val serverUri: String) extends CoreClient {

  private val httpClient = context.system actorOf HttpClient(serverUri, self)

  private var joinedRooms: Set[ClientRoom] = Set()

  import MessageDictionary._
  val onReceive: PartialFunction[Any, Unit] = {
    case CreatePublicRoom =>
      httpClient ! CreatePublicRoom

    case NewJoinedRoom(roomId) =>
      if (joinedRooms map (_ roomId) contains roomId) {
        logger debug s"Room $roomId was already joined!"
      } else {
        joinedRooms += ClientRoom(roomId)
        logger debug s"New joined room $roomId"
      }
      logger debug s"Current joined rooms: $joinedRooms"

    case GetJoinedRooms =>
      sender ! JoinedRooms(joinedRooms)
    }

  override def receive: Receive = onReceive orElse fallbackReceive
}