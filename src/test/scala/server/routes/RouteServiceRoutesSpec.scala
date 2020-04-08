package server.routes

import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import akka.testkit.TestKit
import common.room.SharedRoom.Room
import common._
import common.http.{HttpRequests, Routes}
import common.room.{FilterOptions, IntRoomPropertyValue, RoomJsonSupport, RoomProperty}
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import server.RoomHandler
import server.room.ServerRoom
import server.route_service.RouteService
// Filters on basic room option values: Int, String, Boolean
import common.room.BasicRoomPropertyValueConversions._

import scala.concurrent.ExecutionContextExecutor


class RouteServiceRoutesSpec extends AnyFlatSpec
  with Matchers
  with ScalatestRouteTest
  with RouteCommonTestOptions
  with BeforeAndAfter
  with RoomJsonSupport {

  private implicit val execContext: ExecutionContextExecutor = system.dispatcher
  private val routeService = RouteService(RoomHandler())

  private val route = routeService.route


  behavior of "Route Service routing"

  before {
    //ensure to have at least one room-type
    routeService.addRouteForRoomType(TestRoomType, ServerRoom(_))
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  it should " enable the addition of routes for new rooms type" in {
    getRoomsByTypeWithEmptyFilters ~> route ~> check {
      handled shouldBe true
    }
  }


  it should " reject requests if the given room type  does not exists" in {
    Get("/" + Routes.roomsByType("wrong-type")) ~> route ~> check {
      handled shouldBe false
    }
  }


  it should " reject requests if the given id does not exists" in {
    Get(RoomWithType + "/wrong-id") ~> route ~> check {
      handled shouldBe false
    }
  }

  /// --- Rooms routes ---

  /// GET rooms
  it should "handle GET request on path 'rooms' with room options as payload" in {
    getRoomsWithEmptyFilters ~> route ~> check {
      handled shouldBe true
    }
  }

  /// GET rooms/{type}
  it should "handle GET request on path 'rooms/{type}' with room filters as payload " in {
    val prop1 = RoomProperty("A", 3)
    val f = FilterOptions just prop1 > 2
    getRoomsByTypeWithFilters(f) ~> route ~> check {
      handled shouldBe true
    }
  }


  /// --- POST rooms/{type}
  it should "handle POST request on path 'rooms/{type}' with room properties as payload" in {
    postRoomWithEmptyProperties ~> route ~> check {
      handled shouldBe true
    }
  }

  it should "create only one room after a single POST request" in {
    val testProperty = RoomProperty("A", IntRoomPropertyValue(1))
    createRoomRequest(Set(testProperty))

    getRoomsWithEmptyFilters~> route ~> check {
      responseAs[Seq[Room]] should have size 1
    }

  }

  /// GET rooms/{type}/{id}
  it should "handle GET request on path 'rooms/{type}/{id}' if such id exists " in {
    val room = createRoomRequest()
    Get("/" + Routes.roomByTypeAndId(TestRoomType, room.roomId)) ~> route ~> check { //try to get the created room by id
      handled shouldBe true
    }
  }

  /// --- Web socket  ---
  it should "handle web socket request on path 'connection/{id}'" in {
    val room = createRoomRequest()
    val wsClient = WSProbe()
    WS("/" + Routes.connectionRoute + "/" + room.roomId, wsClient.flow) ~> route ~>
      check {
        isWebSocketUpgrade shouldBe true
      }
  }

  it should "reject web socket request on path 'connection/{id}'" in {
    val wsClient = WSProbe()
    WS("/" + Routes.connectionRoute + "/wrong-id", wsClient.flow) ~> route ~>
      check {
        handled shouldBe false
      }
  }


  def createRoomRequest(testProperties: Set[RoomProperty] = Set.empty): Room = {
    HttpRequests.postRoom("")(TestRoomType, testProperties) ~> route ~> check {
      responseAs[Room]
    }


    /*
  /// PUT rooms/{type}
it should "handle PUT request on path 'rooms/{type}' with room options as payload  " in {
  makeRequestWithEmptyFilter(HttpMethods.PUT)(ROOMS_WITH_TYPE) ~> route ~> check {
    handled shouldBe true
  }
}

it should "handle PUT request on path 'rooms/{type}' with no payload " in {
  Put(ROOMS_WITH_TYPE) ~> route ~> check {
    handled shouldBe true
  }
}

 */
  }


}

