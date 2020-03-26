package server

import akka.http.scaladsl.model.{HttpEntity, HttpMethod, HttpMethods, HttpRequest, MediaTypes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.ByteString
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import server.room.{Room, RoomJsonSupport, RoomSeq}
import server.route_service.RouteService

import scala.concurrent.ExecutionContextExecutor

trait TestOptions {
  val ROOMS = "/" + RouteService.ROOMS_PATH
  val ROOMS_WITH_TYPE = ROOMS + "/" + "type"
  val ROOMS_WITH_TYPE_AND_ID = ROOMS + "/" + "type" + "/" + "0"
  val TEST_ROOM_OPT_JSON = ByteString(
    s"""
       |{
       |  "roomName":"",
       |  "maxClients":0
       |}
        """.stripMargin)

  def makeRequestWithDefaultRoomOptions(method: HttpMethod)(uri: String): HttpRequest = HttpRequest(
    uri = uri, method = method, entity = HttpEntity(MediaTypes.`application/json`, TEST_ROOM_OPT_JSON))
}


class RouteServiceRoutesSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest with TestOptions {

  private implicit val execContext: ExecutionContextExecutor = system.dispatcher
  private val route = RouteService().route

  behavior of "Route Service routing"


  /// --- Rooms routes ---

  /// GET rooms
  it should "handle GET request on path 'rooms' with room options as payload" in {
    makeRequestWithDefaultRoomOptions(HttpMethods.GET)(ROOMS) ~> route ~> check {
      assert(status.isSuccess)
    }
  }

  it should "handle GET request on path 'rooms' with no payload" in {
    Get(ROOMS) ~> route ~> check {
      assert(status.isSuccess)
    }
  }


  /// GET rooms/{type}
  it should "handle GET request on path 'rooms/{type}' with room options as payload " in {
    makeRequestWithDefaultRoomOptions(HttpMethods.GET)(ROOMS_WITH_TYPE) ~> route ~> check {
      assert(status.isSuccess)
    }
  }

  it should "handle GET request on path 'rooms/{type}' with no payload " in {
    Get(ROOMS_WITH_TYPE) ~> route ~> check {
      assert(status.isSuccess)
    }
  }


  /// PUT rooms/{type}
  it should "handle PUT request on path 'rooms/{type}' with room options as payload  " in {
    makeRequestWithDefaultRoomOptions(HttpMethods.PUT)(ROOMS_WITH_TYPE) ~> route ~> check {
      assert(status.isSuccess)
    }
  }

  it should "handle PUT request on path 'rooms/{type}' with no payload " in {
    Put(ROOMS_WITH_TYPE) ~> route ~> check {
      assert(status.isSuccess)
    }
  }


  /// --- POST rooms/{type}
  it should "handle POST request on path 'rooms/{type}' with room options as payload" in {
    makeRequestWithDefaultRoomOptions(HttpMethods.POST)(ROOMS_WITH_TYPE) ~> route ~> check {
      assert(status.isSuccess)
    }
  }


  it should "handle POST request on path 'rooms/{type}' with no payload" in {
    Post(ROOMS_WITH_TYPE) ~> route ~> check {
      assert(status.isSuccess)
    }
  }


  /// GET rooms/{type}/{id}
  it should "handle GET request on path 'rooms/{type}/{id}' " in {
    Get(ROOMS_WITH_TYPE_AND_ID) ~> route ~> check {
      assert(status.isSuccess)
    }
  }


}

