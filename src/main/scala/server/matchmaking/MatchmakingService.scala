package server.matchmaking

import akka.actor.{Actor, Props}
import com.typesafe.scalalogging.LazyLogging
import common.communication.CommunicationProtocol.ProtocolMessage
import common.communication.CommunicationProtocol.ProtocolMessageType._
import common.room.Room.RoomType
import server.RoomHandler
import server.matchmaking.MatchmakingService.{JoinQueue, LeaveQueue, Matchmaker}
import server.room.Client




object MatchmakingService {
  type Matchmaker = List[Client] => Option[Map[Client, Int]]

  trait MatchmakingRequest

  case class JoinQueue(client: Client) extends MatchmakingRequest

  case class LeaveQueue(client: Client) extends MatchmakingRequest


  def apply(matchmaker: Matchmaker, room: RoomType, roomHandler: RoomHandler): Props =
    Props(classOf[MatchmakingService], matchmaker, room, roomHandler)

}

/**
 *
 * @param matchmaker  the matchmaking strategy
 * @param roomType    teh type of room that will be created
 * @param roomHandler the room handler where to spawn the room
 */
class MatchmakingService(private val matchmaker: Matchmaker,
                         private val roomType: RoomType,
                         private val roomHandler: RoomHandler) extends Actor with LazyLogging{

  var clients: Set[Client] = Set.empty

  override def receive: Receive = {
    case JoinQueue(client) =>
      logger.debug("client connected " + client.id)
      this.clients = this.clients + client
      this.applyMatchmakingStrategy()

    case LeaveQueue(client) =>
      logger.debug("client left")
      this.clients = this.clients - client

  }

  // apply the matchmaking strategy to the current list of clients. If the strategy can be applied, the room is
  // created and the clients are removed from the queue
  private def applyMatchmakingStrategy(): Unit = {
    this.matchmaker(this.clients.toList).foreach(grouping => {
      val room = this.roomHandler.createRoom(roomType)
      grouping.keys.foreach(c => c.send(ProtocolMessage(MatchCreated, c.id, room.roomId)))
      this.clients = this.clients -- grouping.keys
    })

  }

}

