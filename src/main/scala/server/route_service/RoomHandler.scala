package server.route_service

import akka.actor.ActorRef
import common.actors.ApplicationActorSystem
import common.{Room, RoomProperty}
import server.room.{RoomActor, ServerRoom}


trait RoomHandler {

  /**
   * @return the list of available rooms
   */
  def availableRooms: List[Room]

  /**
   * create a new room of specific type
   *
   * @param roomType    room type
   * @param roomOptions room options
   * @return the created room
   */
  def createRoom(roomType: String, roomOptions: Option[RoomProperty[Any]]): Room

  /**
   * Get the list of available room of given type.
   * If no rooms exist for the given type creates a new room and
   * return a list containing only the create room.
   *
   * @param roomType    room type
   * @param roomOptions room options for creation
   * @return list of rooms
   */
  def getOrCreate(roomType: String, roomOptions: Option[RoomProperty[Any]]): List[Room]

  /**
   * Return a room with given type and id. Non if does not exists
   *
   * @param roomType room type
   * @param roomId   room id
   * @return option containg the rrom if present, non otherwise
   */
  def getRoomById(roomType: String, roomId: String): Option[Room]

  /**
   * All available rooms filterd by type
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


}

object RoomHandler {
  def apply(): RoomHandler = RoomHandlerImpl()
}

case class RoomHandlerImpl() extends RoomHandler with ApplicationActorSystem {

  var roomTypesHandlers: Map[String, String => ServerRoom] = Map.empty

  //type1 ->  (id->roomActor1), (id2, roomActor2) ...
  //type2 -> (id->roomActor3), (id2, roomActor4) ...
  var roomsByType: Map[String, Map[Room, ActorRef]] = Map.empty


  override def availableRooms: List[Room] = roomsByType.values.flatMap(_.keys).toList

  override def createRoom(roomType: String, roomOptions: Option[RoomProperty[Any]]): Room = {
    this.handleRoomCreation(roomType, roomOptions)
  }

  override def getRoomsByType(roomType: String): List[Room] = this.roomsByType(roomType).keys.toList

  override def getOrCreate(roomType: String, roomOptions: Option[RoomProperty[Any]]): List[Room] =
    this.roomsByType.get(roomType) match {
      case Some(r) => r.keys.toList
      case None => List(createRoom(roomType, roomOptions))
    }

  override def getRoomById(roomType: String, roomId: String): Option[Room] =
    this.roomsByType(roomType).keys.find(_.roomId == roomId)


  override def defineRoomType(roomTypeName: String, roomFactory: String => ServerRoom): Unit = {
    this.roomsByType = this.roomsByType + (roomTypeName -> Map.empty)
    this.roomTypesHandlers = this.roomTypesHandlers + (roomTypeName -> roomFactory)
  }

  private def handleRoomCreation(roomType: String, roomOptions: Option[RoomProperty[Any]]): Room = {
    val roomMap = this.roomsByType(roomType)
    val newId = (roomMap.size + 1).toString
    val roomFactory = this.roomTypesHandlers(roomType)
    val newRoom = roomFactory(newId)
    val newRoomActor = actorSystem actorOf RoomActor(newRoom)
    this.roomsByType = this.roomsByType.updated(roomType, roomMap + (newRoom -> newRoomActor))
    newRoom

  }


}