package server

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Flow
import common.SharedRoom.{Room, RoomId}
import common.communication.BinaryProtocolSerializer
import common.{FilterOptions, RoomProperty, RoomPropertyValue}
import server.room.socket.RoomSocketFlow
import server.room.{RoomActor, ServerRoom}

trait RoomHandler {

  /**
   * Return all available rooms filterd by the given filter options
   *
   * @param filterOptions
   * The filters to be applied. Empty if no specified
   * @return
   * A set of rooms that satisfy the filters
   */
  def getAvailableRooms(filterOptions: FilterOptions = FilterOptions.empty): Seq[Room]

  /**
   * Create a new room of specific type with properties
   *
   * @param roomType       room type
   * @param roomProperties room properties
   * @return the created room
   */
  def createRoom(roomType: String, roomProperties: Set[RoomProperty] = Set.empty): Room

  /**
   * All available rooms filtered by type
   *
   * @param roomType rooms type
   * @return the list of rooms of given type
   */
  def getRoomsByType(roomType: String, filterOptions: FilterOptions = FilterOptions.empty): Seq[Room]

  /**
   * Get specific room with type and id
   *
   * @param roomType rooms type
   * @return the list of rooms of given type
   */
  def getRoomByTypeAndId(roomType: String, roomId: RoomId): Option[Room]

  /**
   * Define a new room type that will be used in room creation.
   *
   * @param roomType    the name of the room's type
   * @param roomFactory the factory to create a room of given type from an id
   */

  def defineRoomType(roomType: String, roomFactory: String => ServerRoom)

  /**
   * Handle new client web socket connection to a room.
   *
   * @param roomId the id of the room the client connects to
   * @return the connection handler if such room id exists
   */
  def handleClientConnection(roomId: RoomId): Option[Flow[Message, Message, Any]]
}

object RoomHandler {
  def apply()(implicit actorSystem: ActorSystem): RoomHandler = RoomHandlerImpl()
}

case class RoomHandlerImpl(implicit actorSystem: ActorSystem) extends RoomHandler {

  var roomTypesHandlers: Map[String, String => ServerRoom] = Map.empty

  //type1 ->  (id->roomActor1), (id2, roomActor2) ...
  //type2 -> (id->roomActor3), (id2, roomActor4) ...
  var roomsByType: Map[String, Map[Room, ActorRef]] = Map.empty

  override def getAvailableRooms(filterOptions: FilterOptions = FilterOptions.empty): Seq[Room] =
    roomsByType.values.flatMap(_ keys).filter(room => {

      // Given a room, check if such room satisfies all filter constraints
      filterOptions.options.forall(filterOption => {
        try {
          val field = room.getClass getDeclaredField filterOption.optionName

          field setAccessible true

          val value = (field get room).asInstanceOf[RoomPropertyValue]
          val filterValue = filterOption.value.asInstanceOf[value.type]

          field setAccessible false

          filterOption.strategy evaluate(value, filterValue)
        } catch {
          // A room is dropped if it doesn't contain the specified field to be used in the filter
          case _: NoSuchFieldException => false
        }
      })
    }).toSeq


  override def createRoom(roomType: String, roomProperties: Set[RoomProperty]): Room = {
    this.handleRoomCreation(roomType, roomProperties)
  }

  override def getRoomByTypeAndId(roomType: String, roomId: RoomId): Option[Room] =
    this.getRoomsByType(roomType).find(_.roomId == roomId)


  override def defineRoomType(roomTypeName: String, roomFactory: String => ServerRoom): Unit = {
    this.roomsByType = this.roomsByType + (roomTypeName -> Map.empty)
    this.roomTypesHandlers = this.roomTypesHandlers + (roomTypeName -> roomFactory)
  }

  /**
   * Creates the socket flow from the client to the room. Messages received from the socket are parsed with
   * a [[common.communication.TextProtocolSerializer]] so they must be [[common.communication.CommunicationProtocol.RoomProtocolMessage]]
   *
   * @param roomId the id of the room the client connects to
   * @return the connection handler if such room id exists
   */
  override def handleClientConnection(roomId: RoomId): Option[Flow[Message, Message, Any]] = {
    this.roomsByType.flatMap(_._2).find(_._1.roomId == roomId)
      .map(option => RoomSocketFlow(option._2, BinaryProtocolSerializer).createFlow())
  }

  override def getRoomsByType(roomType: String, filterOptions: FilterOptions = FilterOptions.empty): Seq[Room] =
    this.roomsByType.get(roomType) match {
      case Some(value) => value.keys.toSeq
      case None => Seq.empty
    }


  private def handleRoomCreation(roomType: String, roomProperties: Set[RoomProperty]): Room = {
    val roomMap = this.roomsByType(roomType)
    val roomFactory = this.roomTypesHandlers(roomType)
    val newRoom = roomFactory(generateUniqueRandomId())
    println(newRoom)
    val newRoomActor = actorSystem actorOf RoomActor(newRoom)
    this.roomsByType = this.roomsByType.updated(roomType, roomMap + (newRoom -> newRoomActor))
    newRoom
  }

  private def generateUniqueRandomId(): RoomId = UUID.randomUUID.toString


}