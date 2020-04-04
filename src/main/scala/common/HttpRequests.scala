package common

import akka.http.scaladsl.model.ws.WebSocketRequest
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import common.SharedRoom.{RoomId, RoomType}

object HttpRequests {

  def getRoomsByType(serverUri: String)(roomType: RoomType): HttpRequest = HttpRequest(
    method = HttpMethods.GET,
    uri = serverUri + "/" + Routes.roomsByType(roomType)
  )

  def putRoomsByType(serverUri: String)(roomType: RoomType): HttpRequest = HttpRequest(
    method = HttpMethods.PUT,
    uri = serverUri + "/" + Routes.roomsByType(roomType)
  )

  def postRoom(serverUri: String)(roomType: RoomType): HttpRequest = HttpRequest(
    method = HttpMethods.POST,
    uri = serverUri + "/" + Routes.roomsByType(roomType)
  )

  def connectToRoom(serverUri: String)(roomId: RoomId): WebSocketRequest =
    WebSocketRequest(s"ws://$serverUri/${Routes.webSocketConnection(roomId)}")
}
