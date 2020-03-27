package server.route_service

import common.Room
import server.room.RoomStrategy

trait RoomHandler{

  def availableRooms: List[Room]

  def createRoom(): Room

  def getRoomById(id: String): Option[Room]

  def defineRoomType(roomTypeName: String, roomStrategy: RoomStrategy)


}

object RoomHandler{
  def apply(): RoomHandler = RoomHandlerImpl()
}
case class RoomHandlerImpl() extends RoomHandler {

  var roomTypesHandlers: Map[String, RoomStrategy] = Map.empty
  var roomsMap: Map[String, Room] = Map.empty

  override def createRoom(): Room = {
    val newId = (this.roomsMap.size + 1).toString
    val newRoom = Room(newId)
    roomsMap = roomsMap + (newId -> newRoom)
    newRoom
  }

  override def getRoomById(id: String): Option[Room] = this.roomsMap.get(id)

  override def availableRooms: List[Room] = roomsMap.values.toList

  override def defineRoomType(roomTypeName: String, roomStrategy: RoomStrategy): Unit =
    roomTypesHandlers = roomTypesHandlers + (roomTypeName -> roomStrategy)
}