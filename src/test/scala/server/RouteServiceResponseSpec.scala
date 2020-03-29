package server

import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.testkit.ScalatestRouteTest
import common.CommonRoom.{Room, RoomJsonSupport, RoomSeq}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import server.route_service.RouteService

import scala.concurrent.ExecutionContextExecutor

class RouteServiceResponseSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest with RoomJsonSupport with TestOptions {


  private implicit val execContext: ExecutionContextExecutor = system.dispatcher
  private val routeService = RouteService()
  private val route = routeService.route
  routeService.addRouteForRoomType(TEST_ROOM_TYPE, EMPTY_ROOM_STRATEGY)

  behavior of "Route Service routing with room handling"



  it should "return a list of available rooms on GET request on path 'rooms'" in {
    makeRequestWithDefaultRoomOptions(HttpMethods.GET)(ROOMS) ~> route ~> check {
      responseAs[RoomSeq]
    }
  }


  it should "return a list of available rooms on GET request on path 'rooms/{type}' " in {
    makeRequestWithDefaultRoomOptions(HttpMethods.GET)(ROOMS_WITH_TYPE) ~> route ~> check {
      responseAs[RoomSeq]
    }
  }

  it should "return a list of available rooms on PUT request on path 'rooms/{type}' " in {
    makeRequestWithDefaultRoomOptions(HttpMethods.PUT)(ROOMS_WITH_TYPE) ~> route ~> check {
      responseAs[RoomSeq]
    }
  }

  it should "return a room that was created on POST request on path 'rooms/{type}' " in {
    makeRequestWithDefaultRoomOptions(HttpMethods.POST)(ROOMS_WITH_TYPE) ~> route ~> check {
      responseAs[Room]
    }
  }

  /*it should "return a room on GET request on path 'rooms/{type}/{id}' " in {
    Get(ROOMS_WITH_TYPE_AND_ID) ~> route ~> check {
      responseAs[Room]
    }
  }*/


}
