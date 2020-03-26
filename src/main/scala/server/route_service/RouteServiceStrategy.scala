package server.route_service

import server.room.{Room, RoomJsonSupport, RoomOptions}

trait RouteServiceStrategy {

  def onGetAllRooms: Option[RoomOptions] => Seq[Room]

  def onGetRoomType: (String, Option[RoomOptions]) => Seq[Room]

  def onPutRoomType: (String, Option[RoomOptions]) => Seq[Room]

  def onPostRoomType: (String, Option[RoomOptions]) => Room

  def onGetRoomTypeId: (String, Int) => Room
}

case class RoomHandlerStrategy(roomHandler: RoomHandler) extends RouteServiceStrategy with RoomJsonSupport {
  override def onGetAllRooms: Option[RoomOptions] => Seq[Room] = _ => Seq.empty

  override def onGetRoomType: (String, Option[RoomOptions]) => Seq[Room] = (_, _) => Seq.empty

  override def onPutRoomType: (String, Option[RoomOptions]) => Seq[Room] = (_, _) => Seq.empty

  override def onPostRoomType: (String, Option[RoomOptions]) => Room = (_, _) => Room(0, RoomOptions("", 0))

  override def onGetRoomTypeId: (String, Int) => Room = (_, _) => Room(0, RoomOptions("", 0))
}
