package server

import akka.http.scaladsl.server.Directives.{complete, get, path, put, _}
import akka.http.scaladsl.server.Route
import spray.json.DefaultJsonProtocol._

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
/*
GET  /rooms/{type}         payload -> filtro sui metadati

GET /rooms/{type}/:id

PUT /rooms/{type}         payload -> metadati

POST /rooms/{type}         payload -> metadati
*/


/**
 * Simple room options for testing
 */
final case class RoomOptions(roomName: String, maxClients: Int){


}

trait RouteService {
  val route: Route
}

object RouteService {
  val ROOMS_PATH = "rooms"

  def apply(): RouteService = RouteServiceImpl(new EmptyRouteServiceStrategy())

}

trait RouteServiceStrategy {
  def onGetAllRooms : RoomOptions => Unit

   def onGetRoomType : (String, RoomOptions) => Unit

   def onPutRoomType: (String, RoomOptions) => Unit

   def onPostRoomType: (String, RoomOptions) => Unit

   def onGetRoomTypeId: (String, Int) => Unit
}

class EmptyRouteServiceStrategy extends RouteServiceStrategy{
  override def onGetAllRooms = _ => {}

  override def onGetRoomType = (_,_) => {}

  override def onPutRoomType = (_,_) => {}

  override def onPostRoomType = (_,_) => {}

  override def onGetRoomTypeId = (_,_) => {}
}

case class RouteServiceImpl(routeServiceStrategy: RouteServiceStrategy) extends RouteService {

  implicit val roomOptionsJsonFormat = jsonFormat2(RoomOptions)


  private val defaultRoute = complete("")

  private val getAllRoomsRoute = pathPrefix(RouteService.ROOMS_PATH) {
    pathEnd {
      get {
        entity(as[RoomOptions]) { roomOptions =>
          routeServiceStrategy.onGetAllRooms(roomOptions)
          complete("GET rooms")
        }
      }
    }
  }

  private def getRoomsByTypeRoute(roomType: String) = get {
    entity(as[RoomOptions]) { roomOptions =>
      routeServiceStrategy.onGetRoomType(roomType, roomOptions)
      complete(s"GET roomType $roomType")
    }
  }

  private def putRoomsByTypeRoute(roomType: String) = put {
    entity(as[RoomOptions]) { roomOptions =>
      routeServiceStrategy.onPutRoomType(roomType, roomOptions)
      complete(s"PUT roomType $roomType")
    }
  }

  private def postRoomsByTypeRoute(roomType: String) = post {
    entity(as[RoomOptions]) { roomOptions =>
      routeServiceStrategy.onPostRoomType(roomType, roomOptions)
      complete(s"POST roomType $roomType")
    }
  }

  private def getRoomByTypeAndId(roomType: String, roomId: Int) = get {
    routeServiceStrategy.onGetRoomTypeId(roomType, roomId)
    complete(s"GET roomType $roomType roomId $roomId")
  }

  val route: Route =
    getAllRoomsRoute ~
      pathPrefix(Segment) { roomType: String =>
        pathEnd {
          getRoomsByTypeRoute(roomType) ~
            putRoomsByTypeRoute(roomType) ~
            postRoomsByTypeRoute(roomType)
        } ~ path(IntNumber) { roomId =>
          getRoomByTypeAndId(roomType, roomId)
        }
      } ~ defaultRoute
}
