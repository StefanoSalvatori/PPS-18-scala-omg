package server.route_service

import java.util.UUID

import common.{FilterOptions, RoomPropertyValue}
import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.Flow
import common.CommonRoom.{Room, RoomId}
import common.actors.ApplicationActorSystem
import common.RoomProperty
import server.room.{RoomActor, ServerRoom}
import server.route_service.RoomHandler.ClientConnectionHandler

trait RoomHandler {

  /**
   * @return the list of available rooms
   */
  def availableRooms: Seq[Room]

  /**
   * FIlter rooms using the specified filters.
   *
   * @param filterOptions
   * The filters to be applied
   * @return
   * A set of rooms that satisfy the filters
   */
  def availableRooms(filterOptions: FilterOptions): Set[Room]

  /**
   * Create a new room of specific type
   *
   * @param roomType    room type
   * @param roomOptions room options
   * @return the created room
   */
  def createRoom(roomType: String, roomOptions: Option[RoomProperty]): Room

  /**
   * Get the list of available room of given type.
   * If no rooms exist for the given type creates a new room and
   * return a list containing only the create room.
   *
   * @param roomType    room type
   * @param roomOptions room options for creation
   * @return list of rooms
   */
  def getOrCreate(roomType: String, roomOptions: Option[RoomProperty]): Seq[Room]

  /**
   * Return a room with given type and id. Non if does not exists
   *
   * @param roomType room type
   * @param roomId   room id
   * @return option containg the rrom if present, non otherwise
   */
  def getRoomById(roomType: String, roomId: String): Option[Room]

  /**
   * All available rooms filtered by type
   *
   * @param roomType rooms type
   * @return the list of rooms of given type
   */
  def getRoomsByType(roomType: String): List[Room]

  /**
   *
   * @param roomType    the name of the room's type
   * @param roomFactory the factory to create given an id
   */

  def defineRoomType(roomType: String, roomFactory: String => ServerRoom)

  /**
   * Handle new client web socket connection to a room.
   *
   * @param roomId the id of the room the client connects to
   * @return the connection handler if such room id exists
   */
  def handleClientConnection(roomId: RoomId): Option[ClientConnectionHandler]
}

object RoomHandler {
  type ClientConnectionHandler = Flow[Message, Message, Any]

  def apply(): RoomHandler = RoomHandlerImpl()
}

case class RoomHandlerImpl() extends RoomHandler {

  import common.actors.ApplicationActorSystem._

  var roomTypesHandlers: Map[String, String => ServerRoom] = Map.empty

  //type1 ->  (id->roomActor1), (id2, roomActor2) ...
  //type2 -> (id->roomActor3), (id2, roomActor4) ...
  var roomsByType: Map[String, Map[Room, ActorRef]] = Map.empty

  override def availableRooms(filterOptions: FilterOptions): Set[Room] =
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
    }).toSet

  override def availableRooms: Seq[Room] = roomsByType.values.flatMap(_.keys).toSeq

  override def createRoom(roomType: String, roomOptions: Option[RoomProperty]): Room = {
    this.handleRoomCreation(roomType, roomOptions)
  }

  override def getRoomsByType(roomType: String): List[Room] = this.roomsByType(roomType).keys.toList

  override def getOrCreate(roomType: String, roomOptions: Option[RoomProperty]): Seq[Room] =
    this.roomsByType(roomType) match {
      //if map is empty no room exists with type 'roomType' so we create a new room and return
      case m: Map[Room, ActorRef] if m.isEmpty => Seq(createRoom(roomType, roomOptions))
      case m => m.keys.toSeq
    }

  override def getRoomById(roomType: String, roomId: String): Option[Room] =
    this.roomsByType(roomType).keys.find(_.roomId == roomId)


  override def defineRoomType(roomTypeName: String, roomFactory: String => ServerRoom): Unit = {
    this.roomsByType = this.roomsByType + (roomTypeName -> Map.empty)
    this.roomTypesHandlers = this.roomTypesHandlers + (roomTypeName -> roomFactory)
  }

  private def handleRoomCreation(roomType: String, roomOptions: Option[RoomProperty]): Room = {
    val roomMap = this.roomsByType(roomType)
    val roomFactory = this.roomTypesHandlers(roomType)
    val newRoom = roomFactory(UUID.randomUUID.toString)
    val newRoomActor = actorSystem actorOf RoomActor(newRoom)
    this.roomsByType = this.roomsByType.updated(roomType, roomMap + (newRoom -> newRoomActor))
    newRoom
  }

  override def handleClientConnection(roomId: RoomId): Option[ClientConnectionHandler] = {
    Some(Flow.fromFunction(_ => TextMessage("foo"))) //TODO: get web socket handler
  }
}