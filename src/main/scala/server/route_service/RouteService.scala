package server.route_service

import akka.http.scaladsl.server.Directives.{complete, get, put, _}
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LazyLogging
import common.{RoomJsonSupport, RoomProperty, Routes}
import server.room.ServerRoom

trait RouteService {
  val route: Route
  var roomTypes: Set[String]

  def addRouteForRoomType(roomTypeName:String, roomFactory: String => ServerRoom)
}

object RouteService {
  def apply(): RouteService = {
    RouteServiceImpl()
  }
}


case class RouteServiceImpl() extends RouteService with RoomJsonSupport with RoomHandling
with LazyLogging {
  this: RoomHandlerService =>

  var roomTypes: Set[String] = Set.empty


  val restHttpRoute = pathPrefix(Routes.publicRooms) {
    pathEnd {
      getAllRoomsRoute
    } ~ pathPrefix(Segment) { roomType: String =>
      if (this.roomTypes.contains(roomType)) {
        pathEnd {
          getRoomsByTypeRoute(roomType) ~
            putRoomsByTypeRoute(roomType) ~
            postRoomsByTypeRoute(roomType)
        } ~ pathPrefix(Segment) { roomId =>
          getRoomByTypeAndId(roomType, roomId)
        }
      } else {
        reject //TODO: how to handle this? Wrong type in rooms/{type}
      }
    }
  }


  /**
   * Handle web socket connection on path /connection/{roomId}
   */
  val webSocketRoute: Route =  pathPrefix(Routes.connectionRoute / Segment) { roomId =>
    get {
        onWebSocketConnection(roomId) match {
          case Some(handler) =>  handleWebSocketMessages(handler)
          case None => reject
        }
      }
  }

  val route: Route = restHttpRoute ~ webSocketRoute


  def addRouteForRoomType(roomTypeName:String, roomFactory: String => ServerRoom): Unit = {
    this.roomTypes = this.roomTypes + roomTypeName
    this.roomHandler.defineRoomType(roomTypeName, roomFactory)
  }

  /**
   * GET rooms/
   */
  private def getAllRoomsRoute: Route =
    get {
      entity(as[RoomProperty]) { roomOptions =>
        val rooms = onGetAllRooms(Some(roomOptions))
        complete(rooms)
      } ~ {
        //if payload is not parsable as room options we just accept the request as with empty room options
        val rooms = onGetAllRooms(Option.empty)
        complete(rooms)
      }
    }


  /**
   * GET rooms/{type}
   */
  private def getRoomsByTypeRoute(roomType: String): Route =
    get {
      entity(as[RoomProperty]) { roomOptions =>
        val rooms = onGetRoomType(roomType, Some(roomOptions))
        complete(rooms)
      } ~ {
        //if payload is not parsable as room options we just accept the request as with empty room options
        val rooms = onGetRoomType(roomType, Option.empty)
        complete(rooms)
      }
    }

  /**
   * PUT rooms/{type}
   */
  private def putRoomsByTypeRoute(roomType: String): Route =
    put {
      entity(as[RoomProperty]) { roomOptions =>
        val rooms = onPutRoomType(roomType, Some(roomOptions))
        complete(rooms) //return a list containing only the created room if no room is available
      } ~ {
        //if payload is not parsable as room options we just accept the request as with empty room options
        val rooms = onPutRoomType(roomType, Option.empty)
        complete(rooms)
      }
    }

  /**
   * POST rooms/{type}
   */
  private def postRoomsByTypeRoute(roomType: String): Route =
    post {
      entity(as[RoomProperty]) { roomOptions =>
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
        case None => reject //TODO: how to handle this? Wrong id in rooms/{type}/{id}
      }
    }


}


