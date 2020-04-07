package server.route_service

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives.{complete, get, _}
import akka.http.scaladsl.server.{Directive1, Route}
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import com.typesafe.scalalogging.LazyLogging
import common.{FilterOptions, RoomJsonSupport, RoomProperty, Routes}
import server.RoomHandler
import server.room.ServerRoom

trait RouteService {
  /**
   * Main route
   */
  val route: Route


  /**
   * Add a route for a new type of rooms.
   * @param roomTypeName room type name used as the route name
   * @param roomFactory a factory to create rooms of that type
   */
  def addRouteForRoomType(roomTypeName:String, roomFactory: String => ServerRoom)
}

object RouteService {
  def apply(roomHandler: RoomHandler): RouteService = new RouteServiceImpl(roomHandler)

}


class RouteServiceImpl(private val roomHandler: RoomHandler) extends RouteService with RoomJsonSupport with LazyLogging {

  private var roomTypes: Set[String] = Set.empty

  /**
   * rest api for rooms.
   */
  def restHttpRoute: Route = pathPrefix(Routes.rooms) {
    pathEnd {
      getAllRoomsRoute
    } ~ pathPrefix(Segment) { roomType: String =>
      if (this.roomTypes.contains(roomType)) {
        pathEnd {
          getRoomsByTypeRoute(roomType) ~
            //putRoomsByTypeRoute(roomType) ~
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
   * Handle web socket connection on path /[[common.Routes#connectionRoute]]/{roomId}
   */
  def webSocketRoute: Route = pathPrefix(Routes.connectionRoute / Segment) { roomId =>
    get {
      this.roomHandler.handleClientConnection(roomId) match {
        case Some(handler) => handleWebSocketMessages(handler)
        case None => reject
      }
    }
  }


  val route: Route = restHttpRoute ~ webSocketRoute


  def addRouteForRoomType(roomTypeName: String, roomFactory: String => ServerRoom): Unit = {
    this.roomTypes = this.roomTypes + roomTypeName
    this.roomHandler.defineRoomType(roomTypeName, roomFactory)
  }

  /**
   * GET rooms/
   */
  private def getAllRoomsRoute: Route =
    get {
      entity(as[FilterOptions]) { filterOptions =>
        val rooms = this.roomHandler.getAvailableRooms(filterOptions)
        complete(rooms)
      }
    }

  /**
   * GET rooms/{type}
   */
  private def getRoomsByTypeRoute(roomType: String): Route =
    get {
      entity(as[FilterOptions]) { filterOptions =>
        val rooms = this.roomHandler.getRoomsByType(roomType, filterOptions)
        complete(rooms)
      }
    }



  /**
   * POST rooms/{type}
   */
  private def postRoomsByTypeRoute(roomType: String): Route =
    post {
      entity(as[Set[RoomProperty]]) { roomProperties =>
        val room = this.roomHandler.createRoom(roomType, roomProperties)
        complete(room)
      }
    }


  /**
   * GET rooms/{type}/{id}
   */
  private def getRoomByTypeAndId(roomType: String, roomId: String): Route =
    get {
      this.roomHandler.getRoomByTypeAndId(roomType, roomId) match {
        case Some(room) => complete(room)
        case None => reject //TODO: how to handle this? Wrong id in rooms/{type}/{id}
      }
    }




  /**
   * PUT rooms/{type}

  private def putRoomsByTypeRoute(roomType: String): Route =
    put {
      entity(as[FilterOptions]) { filterOptions =>
        val rooms = this.roomHandler.getOrCreate(roomType, filterOptions)
        complete(rooms) //return a list containing only the created room if no room is available
      } ~ {
        //if payload is not parsable as room options we just accept the request as with empty room options
        val rooms = this.roomHandler.getOrCreate(roomType)
        complete(rooms)
      }
    }
   */
}




