package server.routes

import akka.http.scaladsl.model.{HttpEntity, HttpMethod, HttpMethods, HttpRequest, MediaTypes}
import akka.util.ByteString
import common.room.SharedRoom.Room
import common.room.{FilterOptions, RoomJsonSupport, RoomProperty}
import common.http.{HttpRequests, Routes}

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
