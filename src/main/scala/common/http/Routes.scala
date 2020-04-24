package common.http

import common.room.Room.{RoomId, RoomType}

object Routes {

  def rooms: String = "rooms"

  /**
   * @return route name for web socket connection with a room
   */
  def connectionRoute: String = "connection"

  /**
   * @return route name for web socket request to enter matchmaking
   */
  def matchmakeRoute: String = "matchmake"

  def roomsByType(roomType: RoomType): String = rooms + "/" + roomType

  def roomByTypeAndId(roomType: RoomType, roomId: RoomId): String = roomsByType(roomType) + "/" + roomId

  def httpUri(address: String, port: Int): String = "http://" + address + ":" + port

  def wsUri(address: String, port: Int): String = "ws://" + address + ":" + port

  def wsUri(httpUri: String): String = httpUri.replace("http", "ws")

  /**
   * @param roomId room id
   * @return route for web socket connection to a room
   */
  def roomSocketConnection(roomId: RoomId): String = connectionRoute + "/" + roomId

  /**
   * @param roomType room type
   * @return route for web socket connection to the matchmaking service
   */
  def matchmakingSocketConnection(roomType: RoomType): String = matchmakeRoute + "/" + roomType
}


