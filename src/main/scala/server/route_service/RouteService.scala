package server.route_service

import akka.http.scaladsl.model.{HttpResponse, StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, get, path, put, _}
import akka.http.scaladsl.server.{PathMatcher, RejectionHandler, Route}
import common.{RoomJsonSupport, RoomOptions, RoomSeq, Routes}
import server.room.RoomStrategy




trait RouteService {
  val route: Route
  var roomTypes: Set[String]
  def addRouteForRoomType(roomType: String, roomStrategy: RoomStrategy)
}

object RouteService {
  def apply(): RouteService = {
    RouteServiceImpl()
  }
}


case class RouteServiceImpl()
  extends RouteService with RoomJsonSupport with RoomHandling {

  this: RoomHandlerService =>
  var roomTypes: Set[String] = Set.empty

  val route: Route =
    pathPrefix(Routes.publicRooms) {
      pathEnd {
        getAllRoomsRoute
      } ~ pathPrefix(Segment) { roomType: String =>
        if(this.roomTypes.contains(roomType)){
          pathEnd {
            getRoomsByTypeRoute(roomType) ~
              putRoomsByTypeRoute(roomType) ~
              postRoomsByTypeRoute(roomType)
          } ~ pathPrefix(Segment) { roomId =>
            getRoomByTypeAndId(roomType, roomId)
          }
        } else {
          reject //TODO: how to handle this?
        }
      }
    }

  def addRouteForRoomType(roomType: String, roomStrategy: RoomStrategy): Unit = {
    this.roomTypes = this.roomTypes + roomType
    this.roomHandler.defineRoomType(roomType, roomStrategy)
  }

  /**
   * GET rooms/
   */
  private def getAllRoomsRoute: Route =
    get {
      entity(as[RoomOptions]) { roomOptions =>
        val rooms = onGetAllRooms(Some(roomOptions))
        complete(RoomSeq(rooms))
      } ~ {
        //if payload is not parsable as room options we just accept the request as with empty room options
        val rooms = onGetAllRooms(Option.empty)
        complete(RoomSeq(rooms))
      }
    }


  /**
   * GET rooms/{type}
   */
  private def getRoomsByTypeRoute(roomType: String): Route =
    get {
      entity(as[RoomOptions]) { roomOptions =>
        val rooms = onGetRoomType(roomType, Some(roomOptions))
        complete(common.RoomSeq(rooms))
      } ~ {
        //if payload is not parsable as room options we just accept the request as with empty room options
        val rooms = onGetRoomType(roomType, Option.empty)
        complete(RoomSeq(rooms))
      }
    }

  /**
   * PUT rooms/{type}
   */
  private def putRoomsByTypeRoute(roomType: String): Route =
    put {
      entity(as[RoomOptions]) { roomOptions =>
        val rooms = onPutRoomType(roomType, Some(roomOptions))
        complete(RoomSeq(rooms)) //return a list containing only the created room if no room is available
      } ~ {
        //if payload is not parsable as room options we just accept the request as with empty room options
        val rooms = onPutRoomType(roomType, Option.empty)
        complete(RoomSeq(rooms))
      }
    }

  /**
   * POST rooms/{type}
   */
  private def postRoomsByTypeRoute(roomType: String): Route =
    post {
      entity(as[RoomOptions]) { roomOptions =>
        val room = onPostRoomType(roomType, Some(roomOptions))
        complete(room)
      } ~ {
        //if payload is not parsable as room options we just accept the request as with empty room options
        val room = onPostRoomType(roomType, Option.empty)
        complete(room)
      }
    }

  /**
   * GET rooms/{type}/{id}
   */
  private def getRoomByTypeAndId(roomType: String, roomId: String): Route =
    get {
      onGetRoomTypeId(roomType, roomId) match {
        case Some(room) => complete(room)
        case None => reject //TODO: how to handle this?
      }
    }



}


