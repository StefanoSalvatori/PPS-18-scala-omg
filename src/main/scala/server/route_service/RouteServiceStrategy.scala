package server.route_service

import common.{Room, RoomJsonSupport, RoomOptions}

trait RouteServiceStrategy {

  /**
   * Handle request for getting all rooms
   * @param roomOptions options for room filtering
   * @return a list of rooms filtered with the room options
   */
  def onGetAllRooms(roomOptions: Option[RoomOptions]): List[Room]

  /**
   * Handle request for getting all rooms of specific type
   * @param roomType room type
   * @param roomOptions roomOptions options for room filtering
   * @return a list of rooms filtered with the room options
   */
  def onGetRoomType(roomType: String, roomOptions: Option[RoomOptions]): List[Room]

  /**
   * Handle request for put room
   * @param roomType room type
   * @param roomOptions roomOptions options for room creation
   * @return a list available rooms or a list containing the only created room if no rooms were available
   */
  def onPutRoomType(roomType: String, roomOptions: Option[RoomOptions]): List[Room]

  /**
   * Handle request for post room
   * @param roomType room type
   * @param roomOptions room options for creation
   * @return the created room
   */
  def onPostRoomType(roomType: String, roomOptions: Option[RoomOptions]): Room

  /**
   * Handle request for getting a room with specific id
   * @param roomType room type
   * @param roomId room id
   * @return An option containing the room with the given id or empty if the  doesn't exists
   */
  def onGetRoomTypeId(roomType: String, roomId: String): Option[Room]

}

trait RoomHandlerService {
  val roomHandler: RoomHandler = RoomHandler()
}
trait RoomHandling extends RouteServiceStrategy with RoomHandlerService with RoomJsonSupport{
  override def onGetAllRooms(roomOptions: Option[RoomOptions]): List[Room] = this.roomHandler.availableRooms

  override def onGetRoomType(roomType: String, roomOptions: Option[RoomOptions]): List[Room]  = this.roomHandler.getRoomsByType(roomType)

  override def onPutRoomType(roomType: String, roomOptions: Option[RoomOptions]): List[Room]  = this.roomHandler.getOrCreate(roomType, roomOptions)

  override def onPostRoomType(roomType: String, roomOptions: Option[RoomOptions]): Room = this.roomHandler.createRoom(roomType, roomOptions)

  override def onGetRoomTypeId(roomType: String, roomId: String):Option[Room] = this.roomHandler.getRoomById(roomType, roomId)
}
