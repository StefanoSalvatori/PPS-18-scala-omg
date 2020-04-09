package common.http

object Routes {

  def rooms: String = "rooms"

  /**
   * @return route name for web socket connection with a room
   */
  def connectionRoute: String = "connection"

  def roomsByType(roomType: String): String = rooms + "/" + roomType

  def roomByTypeAndId(roomType: String, roomId: String): String = roomsByType(roomType) + "/" + roomId

  def httpUri(address: String, port: Int): String = "http://" + address + ":" + port

  def wsUri(address: String, port: Int): String = "ws://" + address + ":" + port

  def wsUri(httpUri: String): String = httpUri.replace("http", "ws")
  /**
   * Route for web socket connection to a room
   *
   * @param roomId room id
   * @return
   */
  def webSocketConnection(roomId: String): String = connectionRoute + "/" + roomId
}


