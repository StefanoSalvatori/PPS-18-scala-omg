package server.route_service

import akka.http.scaladsl.server.Directives.{complete, get, path, put, _}
import akka.http.scaladsl.server.{PathMatcher, Route}
import common.{Room, RoomJsonSupport, RoomOptions, RoomSeq, Routes}

import scala.collection.mutable

trait RouteService {
  val route: Route
  var roomTypes: Set[String]

  def addRouteForRoomType(roomType: String)
}

object RouteService {
  def apply(): RouteService = RouteServiceImpl(Set.empty, RoomHandlerStrategy(RoomHandler(Seq.empty)))
}

case class RoomTypeNotFound() extends Throwable
case class RouteServiceImpl(var roomTypes: Set[String], routeServiceStrategy: RouteServiceStrategy)
  extends RouteService with RoomJsonSupport {

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
          } ~ path(IntNumber) { roomId =>
            getRoomByTypeAndId(roomType, roomId)
          }
        } else {
          failWith(RoomTypeNotFound())
        }

      }
    }

  def addRouteForRoomType(roomType: String): Unit = this.roomTypes = this.roomTypes + roomType

  /**
   * GET rooms/
   */
  private def getAllRoomsRoute: Route =
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
  private def getRoomsByTypeRoute(roomType: String): Route =
    get {
      entity(as[RoomOptions]) { roomOptions =>
        val rooms = this.routeServiceStrategy.onGetRoomType(roomType, Some(roomOptions))
        complete(common.RoomSeq(rooms))
      } ~ {
        //if payload is not parsable as room options we just accept the request as with empty room options
        val rooms = this.routeServiceStrategy.onGetRoomType(roomType, Option.empty)
        complete(RoomSeq(rooms))
      }
    }

  /**
   * PUT rooms/{type}
   */
  private def putRoomsByTypeRoute(roomType: String): Route =
    put {
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
  private def postRoomsByTypeRoute(roomType: String): Route =
    post {
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
  private def getRoomByTypeAndId(roomType: String, roomId: Int): Route =
    get {
      val room = this.routeServiceStrategy.onGetRoomTypeId(roomType, roomId)
      complete(room)
    }


  private def roomTypeMatcher: PathMatcher[Unit] = this.roomTypes.foldLeft(Neutral)(_ | _)




}

/**
 * Stub for a room handler
 */
case class RoomHandler(room: Seq[Room]) {

}
