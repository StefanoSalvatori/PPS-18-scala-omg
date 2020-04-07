package server.routes

import akka.http.scaladsl.model.{HttpEntity, HttpMethod, HttpMethods, HttpRequest, MediaTypes}
import akka.util.ByteString
import common.SharedRoom.Room
import common.{FilterOptions, HttpRequests, RoomJsonSupport, RoomProperty, Routes}

trait RouteCommonTestOptions extends RoomJsonSupport {
  val TestRoomType = "test-room"
  val RoomWithType: String = "/" + Routes.roomsByType(TestRoomType)

  def getRoomsWithEmptyFilters: HttpRequest = {
    HttpRequests.getRooms("")(FilterOptions.empty)
  }

  def getRoomsByTypeWithFilters(filters: FilterOptions): HttpRequest = {
    HttpRequests.getRoomsByType("")(TestRoomType, filters)
  }

  def postRoomWithProperties(properties: Set[RoomProperty]): HttpRequest = {
    HttpRequests.postRoom("")(TestRoomType, properties)
  }


  def getRoomsByTypeWithEmptyFilters: HttpRequest = {
    getRoomsByTypeWithFilters(FilterOptions.empty)
  }

  def postRoomWithEmptyProperties: HttpRequest = {
    postRoomWithProperties(Set.empty)
  }


}
