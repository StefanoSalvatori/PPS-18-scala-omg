package server.route_service

import common.{Room, RoomJsonSupport, RoomOptions}

trait RouteServiceStrategy {

  def onGetAllRooms: Option[RoomOptions] => List[Room]

  def onGetRoomType: (String, Option[RoomOptions]) => List[Room]

  def onPutRoomType: (String, Option[RoomOptions]) => List[Room]

  def onPostRoomType: (String, Option[RoomOptions]) => Room

  def onGetRoomTypeId: (String, String) => Option[Room]
}

case class RouteServiceStrategyImpl(roomHandler: RoomHandler) extends RouteServiceStrategy with RoomJsonSupport {
  override def onGetAllRooms: Option[RoomOptions] => List[Room] = _ => this.roomHandler.availableRooms

  override def onGetRoomType: (String, Option[RoomOptions]) => List[Room] = (_, _) => List.empty

  override def onPutRoomType: (String, Option[RoomOptions]) => List[Room] = (_, _) => List.empty

  override def onPostRoomType: (String, Option[RoomOptions]) => Room = (_, _) => Room("")

  override def onGetRoomTypeId: (String, String) => Option[Room] = (_, id) => this.roomHandler.getRoomById(id)
}
