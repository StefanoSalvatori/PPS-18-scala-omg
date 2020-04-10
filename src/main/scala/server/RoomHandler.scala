package server

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Flow
import common.room.SharedRoom.{Room, RoomId}
import common.room.{FilterOptions, RoomProperty}
import common.room.StringRoomPropertyValue
import common.communication.BinaryProtocolSerializer
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
  def defineRoomType(roomType: String, roomFactory: () => ServerRoom): Unit

  /**
   * Remove the room with the id in input
   *
   * @param roomId the id of the room to remove
   */
  def removeRoom(roomId: RoomId): Unit

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

  private var roomTypesHandlers: Map[String, () => ServerRoom] = Map.empty

  private var roomsByType: Map[String, Map[ServerRoom, ActorRef]] = Map.empty

  override def getAvailableRooms(filterOptions: FilterOptions = FilterOptions.empty): Seq[Room] =
    roomsByType.values.flatMap(_ keys).filter(room =>
      // Given a room, check if such room satisfies all filter constraints
      filterOptions.options forall { filterOption =>
        try {
          val propertyValue = room `valueOf~AsProperty` filterOption.optionName
          val filterValue = filterOption.value.asInstanceOf[propertyValue.type]
          filterOption.strategy evaluate(propertyValue, filterValue)
        } catch {
          // A room is dropped if it doesn't contain the specified field to be used in the filter
          case _: NoSuchFieldException => false
        }
      }
    ).toSeq

  override def createRoom(roomType: String, roomProperties: Set[RoomProperty]): Room = {
    this.handleRoomCreation(roomType, roomProperties)
  }

  override def getRoomByTypeAndId(roomType: String, roomId: RoomId): Option[Room] =
    this.getRoomsByType(roomType).find(_.roomId == roomId)

  override def defineRoomType(roomTypeName: String, roomFactory: () => ServerRoom): Unit = {
    this.roomsByType = this.roomsByType + (roomTypeName -> Map.empty)
    this.roomTypesHandlers = this.roomTypesHandlers + (roomTypeName -> roomFactory)
  }

  override def handleClientConnection(roomId: RoomId): Option[Flow[Message, Message, Any]] = {
    this.roomsByType.flatMap(_._2).find(_._1.roomId == roomId)
      .map(option => RoomSocketFlow(option._2, BinaryProtocolSerializer).createFlow())
  }

  override def getRoomsByType(roomType: String, filterOptions: FilterOptions = FilterOptions.empty): Seq[Room] =
    this.roomsByType.get(roomType) match {
      case Some(value) => value.keys.toSeq
      case None => Seq.empty
    }

  override def removeRoom(roomId: RoomId): Unit = {
    this.roomsByType find (_._2.keys.map(_.roomId) exists (_ == roomId)) foreach (entry => {
      val room = entry._2.find(_._1.roomId == roomId)
      room foreach { r =>
        this.roomsByType = this.roomsByType.updated(entry._1, entry._2 - r._1)
      }
    })
  }

  private def handleRoomCreation(roomType: String, roomProperties: Set[RoomProperty]): Room = {
    val roomMap = this.roomsByType(roomType)
    val newRoom = this.roomTypesHandlers(roomType)()
    val newRoomActor = actorSystem actorOf RoomActor(newRoom, this)
    this.roomsByType = this.roomsByType.updated(roomType, roomMap + (newRoom -> newRoomActor))
    if (roomProperties.map(_ name) contains Room.roomPasswordPropertyName) {
      val splitProperties = roomProperties.groupBy(_.name == Room.roomPasswordPropertyName)
      val password = splitProperties(true)
      val properties = splitProperties.getOrElse(false, Set.empty[RoomProperty])
      newRoom setProperties properties
      newRoom makePrivate password.map(_.value).head.asInstanceOf[StringRoomPropertyValue].value
    } else {
      newRoom setProperties roomProperties
    }
    newRoom
  }

}