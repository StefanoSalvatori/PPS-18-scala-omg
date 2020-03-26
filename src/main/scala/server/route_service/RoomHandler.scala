package server.route_service

import common.Room

trait RoomHandler{

  def availableRooms: List[Room]

  def createRoom(): Room

  def getRoomById(id: String): Option[Room]

}

object RoomHandler{
  def apply(): RoomHandler = RoomHandlerImpl(Map.empty)
}
case class RoomHandlerImpl(var roomsMap: Map[String, Room]) extends RoomHandler {



  override def createRoom(): Room = {
    val newId = (this.roomsMap.size + 1).toString
    val newRoom = Room(newId)
    roomsMap = roomsMap + (newId -> newRoom)
    newRoom
  }

  override def getRoomById(id: String): Option[Room] = this.roomsMap.get(id)

  override def availableRooms: List[Room] = roomsMap.values.toList
}