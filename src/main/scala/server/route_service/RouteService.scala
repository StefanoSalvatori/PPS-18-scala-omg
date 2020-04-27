package server.route_service

import akka.http.scaladsl.server.Directives.{complete, get, _}
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LazyLogging
import common.http.Routes
import common.room.Room.{RoomId, RoomType}
import common.room.{FilterOptions, RoomJsonSupport, RoomProperty}
import server.RoomHandler
import server.matchmaking.{Matchmaker, MatchmakingHandler}
import server.room.ServerRoom

trait RouteService {
  /**
   * Main route
   */
  val route: Route


  /**
   * Add a route for a new type of rooms.
   *
   * @param roomTypeName room type name used as the route name
   * @param roomFactory  a factory to create rooms of that type
   */
  def addRouteForRoomType(roomTypeName: String, roomFactory: () => ServerRoom)

  /**
   * Add a route for a type of room that enable matchmaking
   *
   * @param roomTypeName room type name used as the route name
   */
  def addRouteForMatchmaking[T](roomTypeName: String, roomFactory: () => ServerRoom, matchmaker: Matchmaker[T])
}

object RouteService {
  def apply(roomHandler: RoomHandler, matchmakerHandler: MatchmakingHandler): RouteService =
    new RouteServiceImpl(roomHandler, matchmakerHandler)

}


class RouteServiceImpl(private val roomHandler: RoomHandler,
                       private val matchmakingHandler: MatchmakingHandler) extends RouteService with RoomJsonSupport
  with LazyLogging {

  private var roomTypesRoutes: Set[RoomType] = Set()
  private var matchmakingTypesRoutes: Set[RoomType] = Set()

  override val route: Route = restHttpRoute ~ webSocketRoutes

  override def addRouteForRoomType(roomTypeName: RoomType, roomFactory: () => ServerRoom): Unit = {
    this.roomTypesRoutes = this.roomTypesRoutes + roomTypeName
    this.roomHandler.defineRoomType(roomTypeName, roomFactory)
  }

  override def addRouteForMatchmaking[T](roomTypeName: RoomType, roomFactory: () => ServerRoom,
                                      matchmaker: Matchmaker[T]): Unit = {
    this.matchmakingTypesRoutes = this.matchmakingTypesRoutes + roomTypeName
    this.matchmakingHandler.defineMatchmaker(roomTypeName, matchmaker)
    this.addRouteForRoomType(roomTypeName, roomFactory)
  }

  /**
   * rest api for rooms.
   */
  private def restHttpRoute: Route = pathPrefix(Routes.Rooms) {
    pathEnd {
      getAllRoomsRoute
    } ~ pathPrefix(Segment) { roomType: RoomType =>
      if (this.roomTypesRoutes.contains(roomType)) {
        pathEnd {
          getRoomsByTypeRoute(roomType) ~
            postRoomsByTypeRoute(roomType)
        } ~ pathPrefix(Segment) { roomId =>
          getRoomByTypeAndId(roomType, roomId)
        }
      } else {
        reject
      }
    }
  }

  /**
   * Handel web socket routes
   */
  private def webSocketRoutes: Route = webSocketConnectionRoute ~ matchmakingRoute

  /**
   * Handle web socket connection on path /[[common.http.Routes.ConnectionRoute]]/{roomId}
   */
  private def webSocketConnectionRoute: Route = pathPrefix(Routes.ConnectionRoute / Segment) { roomId =>
    get {
      this.roomHandler.handleClientConnection(roomId) match {
        case Some(handler) => handleWebSocketMessages(handler)
        case None => reject
      }
    }
  }

  /**
   * Handle web socket connection on path /[[common.http.Routes.MatchmakingRoute]]/{type}
   */
  private def matchmakingRoute: Route = pathPrefix(Routes.MatchmakingRoute / Segment) { roomType =>
    get {
      this.matchmakingHandler.handleClientConnection(roomType) match {
        case Some(handler) => handleWebSocketMessages(handler)
        case None => reject
      }
    }
  }

  /**
   * GET rooms/
   */
  private def getAllRoomsRoute: Route =
    get {
      entity(as[FilterOptions]) { filterOptions =>
        val rooms = this.roomHandler.availableRooms(filterOptions)
        complete(rooms)
      }
    }

  /**
   * GET rooms/{type}
   */
  private def getRoomsByTypeRoute(roomType: RoomType): Route =
    get {
      entity(as[FilterOptions]) { filterOptions =>
        val rooms = this.roomHandler.roomsByType(roomType, filterOptions)
        complete(rooms)
      }
    }

  /**
   * POST rooms/{type}
   */
  private def postRoomsByTypeRoute(roomType: RoomType): Route =
    post {
      entity(as[Set[RoomProperty]]) { roomProperties =>
        val room = this.roomHandler.createRoom(roomType, roomProperties)
        complete(room)
      }
    }

  /**
   * GET rooms/{type}/{id}
   */
  private def getRoomByTypeAndId(roomType: RoomType, roomId: RoomId): Route =
    get {
      this.roomHandler.roomByTypeAndId(roomType, roomId) match {
        case Some(room) => complete(room)
        case None => reject
      }
    }


}




