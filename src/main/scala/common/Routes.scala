package common

import akka.http.scaladsl.model.ws.WebSocketRequest
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import common.SharedRoom.{RoomId, RoomType}

object Routes {

  def publicRooms: String = "rooms"

  /**
   * @return route name for web socket connection with a room
   */
  def connectionRoute: String = "connection"

  def roomsByType(roomType: String): String = publicRooms + "/" + roomType

  def roomByTypeAndId(roomType: String, roomId: String): String = roomsByType(roomType) + "/" + roomId

  def uri(address: String, port: Int): String = "http://" + address + ":" + port

  /**
   * Route for web socket connection to a room
   *
   * @param roomId
   * @return
   */
  def webSocketConnection(roomId: String): String = connectionRoute + "/" + roomId
}

object HttpRequests {

  def getRoomsByType(serverUri: String)(roomType: RoomType): HttpRequest = HttpRequest(
    method = HttpMethods.GET,
    uri = serverUri + "/" + Routes.roomsByType(roomType)
  )

  def postRoom(serverUri: String)(roomType: RoomType): HttpRequest = HttpRequest(
    method = HttpMethods.POST,
    uri = serverUri + "/" + Routes.roomsByType(roomType)
  )

  def connectToRoom(serverUri: String)(roomId: RoomId): WebSocketRequest =
    WebSocketRequest("ws://" + serverUri + "/" + Routes.webSocketConnection(roomId))
}
