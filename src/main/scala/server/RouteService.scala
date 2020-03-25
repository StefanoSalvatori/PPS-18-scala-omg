package server

import akka.http.scaladsl.server.Directives.{complete, get, path, put, _}
import akka.http.scaladsl.server.{Route, _}

/*

GET  /rooms/{type}         payload -> filtro sui metadati

GET /rooms/{type}/:id

PUT /rooms/{type}         payload -> metadati

POST /rooms/{type}         payload -> metadati


*/


trait RouteService {
  val route: Route
}

object RouteService {
  type RoomTypeId = String

  val ROOMS_PATH = "rooms"

  def apply(): RouteService = RouteServiceImpl()
}

case class RouteServiceImpl() extends RouteService {

  def onGetAllRooms() = {}

  def onGetRoomType(roomType: String) = {}

  def onPutRoomType(roomType: String) = {}

  def onPostRoomType(roomType: String) = {}

  def onGetRoomTypeId(roomType: String, roomId: Int) = {}

  val route: Route =
    pathPrefix("rooms") {
      pathEnd {
        get {
          onGetAllRooms()
          complete("all rooms")
        }
      } ~ pathPrefix(Segment) { roomType: String =>
        pathEnd {
          get {
            onGetRoomType(roomType)
            complete(s"roomType $roomType")
          } ~ put {
            onPutRoomType(roomType)
            complete(s"roomType $roomType")
          } ~ post {
            onPostRoomType(roomType)
            complete(s"roomType $roomType")
          }
        } ~ path(IntNumber) { roomId =>
          get {
            onGetRoomTypeId(roomType, roomId)
            complete(s"roomType $roomType roomId $roomId")
          }
        }
      }
    } ~ complete("Received something else")
}
