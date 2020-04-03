package server.routes

import akka.http.scaladsl.model.{HttpEntity, HttpMethod, HttpRequest, MediaTypes}
import akka.util.ByteString
import common.Routes

trait RouteCommonTestOptions {
  val TEST_ROOM_TYPE = "test-room"
  val ROOMS: String = "/" + Routes.publicRooms
  val ROOMS_WITH_TYPE: String = "/" + Routes.roomsByType(TEST_ROOM_TYPE)
  val ROOMS_WITH_TYPE_AND_ID: String = "/" + Routes.roomByTypeAndId(TEST_ROOM_TYPE, "a0")
  val TEST_ROOM_OPT_JSON: ByteString = ByteString("")


  def makeRequestWithEmptyFilter(method: HttpMethod)(uri: String): HttpRequest = HttpRequest(
    uri = uri, method = method, entity = HttpEntity(MediaTypes.`application/json`, TEST_ROOM_OPT_JSON))
}
