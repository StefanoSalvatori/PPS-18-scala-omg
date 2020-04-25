package server

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Flow
import common.communication.BinaryProtocolSerializer
import common.room.Room.{RoomId, RoomPassword, RoomType, SharedRoom}
import common.room.{FilterOptions, NoSuchPropertyException, Room, RoomProperty, RoomPropertyValue}
import server.communication.RoomSocket
import server.matchmaking.Group.GroupId
import server.room.{Client, RoomActor, ServerRoom}

trait RoomHandler {

  /**
   * Create a new room of specific type with properties.
   *
   * @param roomType       room type
   * @param roomProperties room properties
   * @return the created room
   */
  def createRoom(roomType: RoomType, roomProperties: Set[RoomProperty] = Set.empty): SharedRoom

  /**
   * Create a new room of specified type with enabled matchmaking.
   * @param roomType room type
   * @param matchmakingGroups client groups to use
   * @return the created room
   */
  def createRoomWithMatchmaking(roomType: RoomType, matchmakingGroups: Map[Client, GroupId]): SharedRoom

  /**
   * All available rooms filtered by type.
   *
   * @param roomType rooms type
   * @return the list of rooms of given type
   */
  def roomsByType(roomType: RoomType, filterOptions: FilterOptions = FilterOptions.empty): Seq[SharedRoom]

  /**
   * Return all available rooms filtered by the given filter options.
   *
   * @param filterOptions
   * The filters to be applied. Empty if no specified
   * @return
   * A set of rooms that satisfy the filters
   */
  def availableRooms(filterOptions: FilterOptions = FilterOptions.empty): Seq[SharedRoom]

  /**
   * Get specific room with type and id.
   *
   * @param roomType rooms type
   * @return the list of rooms of given type
   */
  def roomByTypeAndId(roomType: RoomType, roomId: RoomId): Option[SharedRoom]

  /**
   * Define a new room type that will be used in room creation.
   *
   * @param roomType    the name of the room's type
   * @param roomFactory the factory to create a room of given type from an id
   */
  def defineRoomType(roomType: RoomType, roomFactory: () => ServerRoom): Unit

  /**
   * Remove the room with the id in input.
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

  private var roomTypesHandlers: Map[RoomType, () => ServerRoom] = Map.empty

  private var _roomsByType: Map[RoomType, Map[ServerRoom, ActorRef]] = Map.empty
  private var _roomsWithMatchmakingByType: Map[RoomType, Map[ServerRoom, ActorRef]] = Map.empty

  override def createRoom(roomType: RoomType, roomProperties: Set[RoomProperty]): SharedRoom = {
    val newRoom = roomTypesHandlers(roomType)()
    val newRoomActor = actorSystem actorOf RoomActor(newRoom, this)
    _roomsByType = _roomsByType + (
      roomType -> (_roomsByType(roomType) + (newRoom -> newRoomActor))
      )
    // Set property and password
    if (roomProperties.map(_ name) contains Room.roomPasswordPropertyName) {
      val splitProperties = roomProperties.groupBy(_.name == Room.roomPasswordPropertyName)
      val password = splitProperties(true)
      val properties = splitProperties.getOrElse(false, Set.empty[RoomProperty])
      newRoom setProperties properties
      newRoom makePrivate RoomPropertyValue.valueOf(password.map(_.value).head).asInstanceOf[RoomPassword]
    } else {
      newRoom setProperties roomProperties
    }
    newRoom
  }

  override def createRoomWithMatchmaking(roomType: RoomType,
                                         matchmakingGroups: Map[Client, GroupId]): SharedRoom = synchronized {
    val newRoom = roomTypesHandlers(roomType)()
    val newRoomActor = actorSystem actorOf RoomActor(newRoom, this)
    _roomsWithMatchmakingByType = _roomsWithMatchmakingByType + (
      roomType -> (_roomsWithMatchmakingByType(roomType) + (newRoom -> newRoomActor))
      )
    newRoom setGroups matchmakingGroups
    newRoom
  }

  override def roomsByType(roomType: RoomType, filterOptions: FilterOptions = FilterOptions.empty): Seq[SharedRoom] =
    _roomsByType get roomType match {
      case Some(value) =>
        value.keys
          .filter(this filterRoomsWith filterOptions)
          .filterNot(_ isLocked)
          .filterNot(_ isMatchmakingEnabled)
          .toSeq
      case None => Seq.empty
    }

  override def availableRooms(filterOptions: FilterOptions = FilterOptions.empty): Seq[SharedRoom] =
    _roomsByType.keys.flatMap(roomType => roomsByType(roomType, filterOptions)).toSeq

  override def roomByTypeAndId(roomType: RoomType, roomId: RoomId): Option[SharedRoom] =
    roomsByType(roomType).find(_.roomId == roomId)

  override def defineRoomType(roomTypeName: RoomType, roomFactory: () => ServerRoom): Unit = {
    this._roomsByType = this._roomsByType + (roomTypeName -> Map.empty)
    this.roomTypesHandlers = this.roomTypesHandlers + (roomTypeName -> roomFactory)
  }

  override def removeRoom(roomId: RoomId): Unit = {
    this._roomsByType find (_._2.keys.map(_.roomId) exists (_ == roomId)) foreach (entry => {
      val room = entry._2.find(_._1.roomId == roomId)
      room foreach { r =>
        this._roomsByType = this._roomsByType.updated(entry._1, entry._2 - r._1)
      }
    })
  }

  override def handleClientConnection(roomId: RoomId): Option[Flow[Message, Message, Any]] = {
    this._roomsByType
      .flatMap(_._2)
      .find(_._1.roomId == roomId)
      .map(room => RoomSocket(room._2, BinaryProtocolSerializer(), room._1.socketConfigurations).open())
  }

  /**
   * It checks filter constraints on a given room.
   * @param filterOptions room properties to check
   * @return the filter to be applied
   */
  private def filterRoomsWith(filterOptions: FilterOptions): ServerRoom => Boolean = room => {
    filterOptions.options forall { filterOption =>
      try {
        val propertyValue = room `valueOf~AsPropertyValue` filterOption.optionName
        val filterValue = filterOption.value.asInstanceOf[propertyValue.type]
        filterOption.strategy evaluate(propertyValue, filterValue)
      } catch {
        // A room is dropped if it doesn't contain the specified property to be used in the filter
        case _: NoSuchPropertyException => false
      }
    }
  }
}