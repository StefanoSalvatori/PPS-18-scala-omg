package common

object Routes {

  def publicRooms: String = "rooms"

  /**
   * @return route name for web socket connection with a room
   */
  def connectionRoute: String = "connection"

  def roomsByType(roomType: String): String = publicRooms + "/" +  roomType

  def roomByTypeAndId(roomType: String, roomId: String): String = roomsByType(roomType) + "/" + roomId

  def uri(address: String, port: Int): String = "http://" + address + ":" + port

  /**
   * Route for web socket connection to a room
   * @param roomId
   * @return
   */
  def webSocketConnection(roomId: String): String = connectionRoute + "/" + roomId
}
