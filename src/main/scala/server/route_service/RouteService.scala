package server.route_service

import akka.http.scaladsl.server.Directives.{complete, get, path, put, _}
import akka.http.scaladsl.server.Route
import server.room.{Room, RoomJsonSupport, RoomOptions, RoomSeq}

trait RouteService {
  val route: Route
}

object RouteService {
  val ROOMS_PATH = "rooms"

  def apply(): RouteService = RouteServiceImpl(RoomHandlerStrategy(RoomHandler(Seq.empty)))

}


case class RouteServiceImpl(routeServiceStrategy: RouteServiceStrategy) extends RouteService with RoomJsonSupport {

  /**
   * GET rooms/
   */
  private def getAllRoomsRoute =
    get {
      entity(as[RoomOptions]) { roomOptions =>
        val rooms = this.routeServiceStrategy.onGetAllRooms(Some(roomOptions))
        complete(RoomSeq(rooms))
      } ~ {
        //if payload is not parsable as room options we just accept the request as with empty room options
        val rooms = this.routeServiceStrategy.onGetAllRooms(Option.empty)
        complete(RoomSeq(rooms))
      }
    }


  /**
   * GET rooms/{type}
   */
  private def getRoomsByTypeRoute(roomType: String) = get {
    entity(as[RoomOptions]) { roomOptions =>
      val rooms = this.routeServiceStrategy.onGetRoomType(roomType, Some(roomOptions))
      complete(RoomSeq(rooms))
    } ~ {
      //if payload is not parsable as room options we just accept the request as with empty room options
      val rooms = this.routeServiceStrategy.onGetRoomType(roomType, Option.empty)
      complete(RoomSeq(rooms))
    }
  }

  /**
   * PUT rooms/{type}
   */
  private def putRoomsByTypeRoute(roomType: String) = put {
    entity(as[RoomOptions]) { roomOptions =>
      val rooms = this.routeServiceStrategy.onPutRoomType(roomType, Some(roomOptions))
      complete(RoomSeq(rooms)) //return a list containing only the created room if no room is available
    } ~ {
      //if payload is not parsable as room options we just accept the request as with empty room options
      val rooms = this.routeServiceStrategy.onPutRoomType(roomType, Option.empty)
      complete(RoomSeq(rooms))
    }
  }

  /**
   * POST rooms/{type}
   */
  private def postRoomsByTypeRoute(roomType: String) = post {
    entity(as[RoomOptions]) { roomOptions =>
      val room = this.routeServiceStrategy.onPostRoomType(roomType, Some(roomOptions))
      complete(room)
    } ~ {
      //if payload is not parsable as room options we just accept the request as with empty room options
      val room = this.routeServiceStrategy.onPostRoomType(roomType, Option.empty)
      complete(room)
    }
  }

  /**
   * GET rooms/{type}/{id}
   */
  private def getRoomByTypeAndId(roomType: String, roomId: Int) = get {
    val room = this.routeServiceStrategy.onGetRoomTypeId(roomType, roomId)
    complete(room)
  }


  val route: Route =
    pathPrefix(RouteService.ROOMS_PATH) {
      pathEnd {
        getAllRoomsRoute
      } ~
        pathPrefix(Segment) { roomType: String =>
        pathEnd {
          getRoomsByTypeRoute(roomType) ~
            putRoomsByTypeRoute(roomType) ~
            postRoomsByTypeRoute(roomType)
        } ~ path(IntNumber) { roomId =>
          getRoomByTypeAndId(roomType, roomId)
        }
      }
    }
}

/**
 * Stub for a room handler
 */
case class RoomHandler(room: Seq[Room]) {

}
