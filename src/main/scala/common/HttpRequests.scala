package common

import akka.http.scaladsl.model.ws.WebSocketRequest
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import common.SharedRoom.{RoomId, RoomType}
import spray.json.{JsValue, JsonFormat, JsonReader, JsonWriter, RootJsonFormat}

object HttpRequests extends RoomJsonSupport {

  private val defaultContentType = ContentTypes.`application/json`
  implicit private def payloadContentCreator[T](value: T)(implicit jsonFormatter: RootJsonFormat[T]): String =
    jsonFormatter write value toString

  def getRoomsByType(serverUri: String)(roomType: RoomType, filterOptions: FilterOptions): HttpRequest = HttpRequest(
    method = HttpMethods.GET,
    uri = serverUri + "/" + Routes.roomsByType(roomType),
    entity = HttpEntity(defaultContentType, filterOptions)
  )

  def putRoomsByType(serverUri: String)(roomType: RoomType, properties: Set[RoomProperty]): HttpRequest = HttpRequest(
    method = HttpMethods.PUT,
    uri = serverUri + "/" + Routes.roomsByType(roomType),
    entity = HttpEntity(defaultContentType, properties)
  )

  def postRoom(serverUri: String)(roomType: RoomType, properties: Set[RoomProperty]): HttpRequest = HttpRequest(
    method = HttpMethods.POST,
    uri = serverUri + "/" + Routes.roomsByType(roomType),
    entity = HttpEntity(defaultContentType, properties)
  )

  def connectToRoom(serverUri: String)(roomId: RoomId): WebSocketRequest =
    WebSocketRequest(s"ws://$serverUri/${Routes.webSocketConnection(roomId)}")
}