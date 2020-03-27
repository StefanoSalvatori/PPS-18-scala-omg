package server

import akka.http.scaladsl.model.{HttpEntity, HttpMethod, HttpMethods, HttpRequest, MediaTypes, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.ByteString
import common.Routes
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import server.room.RoomStrategy
import server.route_service.RouteService

import scala.concurrent.ExecutionContextExecutor

trait TestOptions {
  val TEST_ROOM_TYPE = "test-room"
  val ROOMS: String = "/" + Routes.publicRooms
  val ROOMS_WITH_TYPE: String = "/" + Routes.roomsByType(TEST_ROOM_TYPE)
  val ROOMS_WITH_TYPE_AND_ID: String = "/" + Routes.roomByTypeAndId(TEST_ROOM_TYPE, "a0")
  val TEST_ROOM_OPT_JSON: ByteString = ByteString(
    s"""
       |{
       |  "id":0
       |}
        """.stripMargin)
  val EMPTY_ROOM: RoomStrategy = new RoomStrategy {
    override def onJoin(): Unit = {}
    override def onMessageReceived(): Unit = {}
    override def onLeave(): Unit = {}
    override def onCreate(): Unit = {}
  }

  def makeRequestWithDefaultRoomOptions(method: HttpMethod)(uri: String): HttpRequest = HttpRequest(
    uri = uri, method = method, entity = HttpEntity(MediaTypes.`application/json`, TEST_ROOM_OPT_JSON))
}


class RouteServiceRoutesSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest with TestOptions
  with BeforeAndAfter {

  private implicit val execContext: ExecutionContextExecutor = system.dispatcher
  private val routeService = RouteService()

  private val route = routeService.route


  behavior of "Route Service routing"

  before {
    //ensure to have at least one room-type
    routeService.addRouteForRoomType(TEST_ROOM_TYPE, EMPTY_ROOM)

  }

  it should " enable the addition of routes for new rooms type" in {
    assert(routeService.roomTypes.contains(TEST_ROOM_TYPE))
  }


  it should " reject requests if the given room type  does not exists" in {
    Get("/" + Routes.roomsByType("wrong-type")) ~> route ~> check {
      handled shouldBe false
    }
  }


  it should " reject requests if the given id does not exists" in {
    Get(ROOMS_WITH_TYPE + "/wrong-id" ) ~> route ~> check {
      handled shouldBe false
    }
  }

  /// --- Rooms routes ---

  /// GET rooms
  it should "handle GET request on path 'rooms' with room options as payload" in {
    makeRequestWithDefaultRoomOptions(HttpMethods.GET)(ROOMS) ~> route ~> check {
      handled shouldBe true
    }
  }

  it should "handle GET request on path 'rooms' with no payload" in {
    Get(ROOMS) ~> route ~> check {
      handled shouldBe true
    }
  }


  /// GET rooms/{type}
  it should "handle GET request on path 'rooms/{type}' with room options as payload " in {
    makeRequestWithDefaultRoomOptions(HttpMethods.GET)(ROOMS_WITH_TYPE) ~> route ~> check {
      handled shouldBe true
    }
  }

  it should "handle GET request on path 'rooms/{type}' with no payload " in {
    Get(ROOMS_WITH_TYPE) ~> route ~> check {
      handled shouldBe true
    }
  }


  /// PUT rooms/{type}
  it should "handle PUT request on path 'rooms/{type}' with room options as payload  " in {
    makeRequestWithDefaultRoomOptions(HttpMethods.PUT)(ROOMS_WITH_TYPE) ~> route ~> check {
      handled shouldBe true
    }
  }

  it should "handle PUT request on path 'rooms/{type}' with no payload " in {
    Put(ROOMS_WITH_TYPE) ~> route ~> check {
      handled shouldBe true
    }
  }


  /// --- POST rooms/{type}
  it should "handle POST request on path 'rooms/{type}' with room options as payload" in {
    makeRequestWithDefaultRoomOptions(HttpMethods.POST)(ROOMS_WITH_TYPE) ~> route ~> check {
      handled shouldBe true
    }
  }


  it should "handle POST request on path 'rooms/{type}' with no payload" in {
    Post(ROOMS_WITH_TYPE) ~> route ~> check {
      handled shouldBe true
    }
  }


 /* /// GET rooms/{type}/{id}
  it should "handle GET request on path 'rooms/{type}/{id}' " in {
    Get(ROOMS_WITH_TYPE_AND_ID) ~> route ~> check {
      handled shouldBe true
    }
  }*/


}

