package server.route_service

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Directives.handleWebSocketMessages
import akka.stream.scaladsl.{Flow, Sink, Source}
import common.CommonRoom.{Room, RoomJsonSupport, RoomOptions}
import server.room.ServerRoom.{RoomId, RoomType}
import server.route_service.RoomHandler.ClientConnectionHandler

trait RouteServiceStrategy {

  /**
   * Handle request for getting all rooms
   *
   * @param roomOptions options for room filtering
   * @return a list of rooms filtered with the room options
   */
  def onGetAllRooms(roomOptions: Option[RoomOptions]): List[Room]

  /**
   * Handle request for getting all rooms of specific type
   *
   * @param roomType    room type
   * @param roomOptions roomOptions options for room filtering
   * @return a list of rooms filtered with the room options
   */
  def onGetRoomType(roomType: RoomType, roomOptions: Option[RoomOptions]): List[Room]

  /**
   * Handle request for put room
   *
   * @param roomType    room type
   * @param roomOptions roomOptions options for room creation
   * @return a list available rooms or a list containing the only created room if no rooms were available
   */
  def onPutRoomType(roomType: RoomType, roomOptions: Option[RoomOptions]): List[Room]

  /**
   * Handle request for post room
   *
   * @param roomType    room type
   * @param roomOptions room options for creation
   * @return the created room
   */
  def onPostRoomType(roomType: RoomType, roomOptions: Option[RoomOptions]): Room

  /**
   * Handle request for getting a room with specific id
   *
   * @param roomType room type
   * @param roomId   room id
   * @return An option containing the room with the given id or empty if the  doesn't exists
   */
  def onGetRoomTypeId(roomType: RoomType, roomId: RoomId): Option[Room]

  def onWebSocketConnection(roomId: RoomId): Option[ClientConnectionHandler]

}

trait RoomHandlerService {
  val roomHandler: RoomHandler = RoomHandler()
}

trait RoomHandling extends RouteServiceStrategy with RoomHandlerService with RoomJsonSupport {

  override def onGetAllRooms(roomOptions: Option[RoomOptions]): List[Room] = this.roomHandler.availableRooms

  override def onGetRoomType(roomType: RoomType, roomOptions: Option[RoomOptions]): List[Room]
  = this.roomHandler.getRoomsByType(roomType)

  override def onPutRoomType(roomType: RoomType, roomOptions: Option[RoomOptions]): List[Room] =
    this.roomHandler.getOrCreate(roomType, roomOptions)

  override def onPostRoomType(roomType: RoomType, roomOptions: Option[RoomOptions]): Room =
    this.roomHandler.createRoom(roomType, roomOptions)

  override def onGetRoomTypeId(roomType: RoomType, roomId: RoomId): Option[Room] =
    this.roomHandler.getRoomById(roomType, roomId)

  override def onWebSocketConnection(roomId: RoomId): Option[ClientConnectionHandler] =
    this.roomHandler.handleClientConnection(roomId)
}
