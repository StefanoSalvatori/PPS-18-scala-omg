package server

import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.testkit.ScalatestRouteTest
import common.CommonRoom.{Room, RoomJsonSupport}
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import server.room.ServerRoom.RoomStrategy
import server.route_service.RouteService

import scala.concurrent.ExecutionContextExecutor

class RouteServiceResponseSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest with RoomJsonSupport
  with TestOptions
  with BeforeAndAfter {


  private implicit val execContext: ExecutionContextExecutor = system.dispatcher
  private var routeService = RouteService()
  private var route = routeService.route


  behavior of "Route Service routing with room handling"

  before {
    routeService = RouteService()
    route = routeService.route

    //define two room type for test
    routeService.addRouteForRoomType(TEST_ROOM_TYPE, RoomStrategy.empty)
  }

  it should "respond with a list of available rooms on GET request on path 'rooms'" in {
    makeRequestWithDefaultRoomOptions(HttpMethods.GET)(ROOMS) ~> route ~> check {
      responseAs[Seq[Room]]
    }
  }


  it should "respond with a list of available rooms on GET request on path 'rooms/{type}' " in {
    makeRequestWithDefaultRoomOptions(HttpMethods.GET)(ROOMS_WITH_TYPE) ~> route ~> check {
      responseAs[Seq[Room]]
    }
  }

  it should "respond with a list of available rooms on PUT request on path 'rooms/{type}' " in {
    makeRequestWithDefaultRoomOptions(HttpMethods.PUT)(ROOMS_WITH_TYPE) ~> route ~> check {
      responseAs[Seq[Room]]
    }
  }

  it should "respond with a room that was created on POST request on path 'rooms/{type}' " in {
    makeRequestWithDefaultRoomOptions(HttpMethods.POST)(ROOMS_WITH_TYPE) ~> route ~> check {
      responseAs[Room]
    }
  }

  it should "respond with an empty sequence if no rooms have been created " in {
    makeRequestWithDefaultRoomOptions(HttpMethods.GET)(ROOMS_WITH_TYPE) ~> route ~> check {
      responseAs[Seq[Room]] shouldBe empty
    }
  }

  it should "respond with all the rooms of the requested type on GET request on path 'rooms/{type}' " in {
    makeRequestWithDefaultRoomOptions(HttpMethods.POST)(ROOMS_WITH_TYPE) ~> route ~> check {}
    makeRequestWithDefaultRoomOptions(HttpMethods.POST)(ROOMS_WITH_TYPE) ~> route ~> check {}

    makeRequestWithDefaultRoomOptions(HttpMethods.GET)(ROOMS_WITH_TYPE) ~> route ~> check {
      responseAs[Seq[Room]] should have size 2
    }
  }

  it should "create a new room and respond with that room after PUT request if no room exists with the given type  " in {
    makeRequestWithDefaultRoomOptions(HttpMethods.PUT)(ROOMS_WITH_TYPE) ~> route ~> check {
      responseAs[Seq[Room]] should have size 1
    }

    //ensure creation
    makeRequestWithDefaultRoomOptions(HttpMethods.GET)(ROOMS_WITH_TYPE) ~> route ~> check {
      responseAs[Seq[Room]] should have size 1
    }
  }

  /*it should "return a room on GET request on path 'rooms/{type}/{id}' " in {
    Get(ROOMS_WITH_TYPE_AND_ID) ~> route ~> check {
      responseAs[Room]
    }
  }*/


}
