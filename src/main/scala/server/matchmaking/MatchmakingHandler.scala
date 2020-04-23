package server.matchmaking

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Flow
import common.communication.BinaryProtocolSerializer
import common.room.Room.RoomType
import server.RoomHandler
import server.communication.MatchmakingSocket
import server.matchmaking.MatchmakingService.{MatchmakingStrategy}
import server.room.ServerRoom

trait MatchmakingHandler {

  /**
   * Define a new matchmaker for the room type
   * @param roomType room type
   * @param matchmaker matchmaker
   */
  def defineMatchmaker(roomType: RoomType, matchmaker: MatchmakingStrategy)

  /**
   * Handle a client request for the matchmaker for the given type
   * @param roomType room type
   * @return the flow for the socket communication
   */
  def handleClientConnection(roomType: RoomType): Option[Flow[Message, Message, Any]]

}

object MatchmakingHandler {
  def apply(roomHandler: RoomHandler) (implicit actorSystem: ActorSystem): MatchmakingHandler =
    new MatchmakingHandlerImpl(roomHandler)
}


class MatchmakingHandlerImpl(private val roomHandler: RoomHandler)
                            (implicit actorSystem: ActorSystem) extends MatchmakingHandler {
  private var matchmakers: Map[RoomType, ActorRef] = Map()

  override def defineMatchmaker(roomType: RoomType, matchmaker: MatchmakingStrategy): Unit = {
    val matchmakingService = actorSystem actorOf MatchmakingService(matchmaker, roomType, this.roomHandler)
    this.matchmakers = this.matchmakers.updated(roomType, matchmakingService)
  }

  override def handleClientConnection(roomType: RoomType): Option[Flow[Message, Message, Any]] = {
    this.matchmakers.get(roomType).map(MatchmakingSocket(_, BinaryProtocolSerializer()).open())
  }
}