package server.route_service

import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}
import common.CommonRoom.{Room, RoomId, RoomOptions}
import server.room.ServerRoom.RoomStrategy
import server.route_service.RoomHandler.ClientConnectionHandler

import scala.concurrent.Future


trait RoomHandler {

  /**
   * @return the list of available rooms
   */
  def availableRooms: Seq[Room]

  /**
   * create a new room of specific type
   *
   * @param roomType    room type
   * @param roomOptions room options
   * @return the created room
   */
  def createRoom(roomType: String, roomOptions: Option[RoomOptions]): Room

  /**
   * Get the list of available room of given type.
   * If no rooms exist for the given type creates a new room and
   * return a list containing only the create room.
   *
   * @param roomType    room type
   * @param roomOptions room options for creation
   * @return list of rooms
   */
  def getOrCreate(roomType: String, roomOptions: Option[RoomOptions]): Seq[Room]

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
  def getRoomsByType(roomType: String): Seq[Room]

  /**
   * Define a new room type to handle on room creation.
   * New rooms with this type are created with the given room strategy
   *
   * @param roomType     room type
   * @param roomStrategy room strategy
   */
  def defineRoomType(roomType: String, roomStrategy: RoomStrategy)


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

  var roomTypesHandlers: Map[String, RoomStrategy] = Map.empty

  //type1 ->  (id->room), (id2, room2) ...
  //type2 -> (id->room), (id2, room2) ...
  var roomsByType: Map[String, Map[String, Room]] = Map.empty


  override def availableRooms: Seq[Room] = roomsByType.values.flatMap(_.values).toSeq

  override def createRoom(roomType: String, roomOptions: Option[RoomOptions]): Room = {
    this.handleRoomCreation(roomType, roomOptions)
  }

  override def getRoomsByType(roomType: String): Seq[Room] =
    this.roomsByType(roomType).values.toSeq

  override def getOrCreate(roomType: String, roomOptions: Option[RoomOptions]): Seq[Room] =
    this.roomsByType(roomType) match {
      //if map is empty no room exists with type: roomType so we create a new room
      case m: Map[String, Room] if m.isEmpty => Seq(createRoom(roomType, roomOptions))
      case m => m.values.toSeq


    }

  override def getRoomById(roomType: String, roomId: String): Option[Room] =
    this.roomsByType(roomType).get(roomId)


  override def defineRoomType(roomTypeName: String, roomStrategy: RoomStrategy): Unit = {
    this.roomsByType = this.roomsByType + (roomTypeName -> Map.empty)
    this.roomTypesHandlers = this.roomTypesHandlers + (roomTypeName -> roomStrategy)
  }

  private def handleRoomCreation(roomType: String, roomOptions: Option[RoomOptions]): Room = {
    val roomMap = this.roomsByType(roomType)
    val newId = (roomMap.size + 1).toString
    val newRoom = Room(newId)
    val newRoomMap = roomMap + (newId -> newRoom)
    this.roomsByType = this.roomsByType.updated(roomType, newRoomMap)
    newRoom
  }

  override def handleClientConnection(roomId: RoomId): Option[ClientConnectionHandler] = {
    Some(Flow.fromFunction(_ => TextMessage("foo")))
  }
}