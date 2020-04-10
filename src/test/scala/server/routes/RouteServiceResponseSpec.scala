package server.routes

import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestKit
import common.http.HttpRequests
import common.room.SharedRoom.Room
import common.room.{RoomJsonSupport, RoomProperty}
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import server.RoomHandler
import server.room.ServerRoom
import server.route_service.RouteService

import scala.concurrent.ExecutionContextExecutor

class RouteServiceResponseSpec extends AnyFlatSpec with Matchers
  with ScalatestRouteTest
  with RoomJsonSupport
  with RouteCommonTestOptions
  with BeforeAndAfter {


  private implicit val execContext: ExecutionContextExecutor = system.dispatcher
  private var routeService = RouteService(RoomHandler())
  private var route = routeService.route


  behavior of "Route Service routing with room handling"

  before {
    routeService = RouteService(RoomHandler())
    route = routeService.route

    //define two room type for test
    routeService.addRouteForRoomType(TestRoomType, () => ServerRoom())
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }


  it should "respond with a list of available rooms on GET request on path 'rooms'" in {
    getRoomsWithEmptyFilters ~> route ~> check {
      responseAs[Seq[Room]]
    }
  }


  it should "respond with a list of available rooms on GET request on path 'rooms/{type}' " in {
    getRoomsByTypeWithEmptyFilters ~> route ~> check {
      responseAs[Seq[Room]]
    }
  }


  it should "respond with a room that was created on POST request on path 'rooms/{type}' " in {
    postRoomWithEmptyProperties ~> route ~> check {
      responseAs[Room]
    }
  }

  it should "respond with an empty sequence if no rooms have been created " in {
    getRoomsByTypeWithEmptyFilters ~> route ~> check {
      responseAs[Seq[Room]] shouldBe empty
    }
  }

  it should "respond with all the rooms of the requested type on GET request on path 'rooms/{type}' " in {
    createRoomRequest(Set.empty)
    createRoomRequest(Set.empty)

    getRoomsByTypeWithEmptyFilters ~> route ~> check {
      responseAs[Seq[Room]] should have size 2
    }
  }


  def createRoomRequest(testProperties: Set[RoomProperty] = Set.empty): Room = {
    HttpRequests.postRoom("")(TestRoomType, testProperties) ~> route ~> check {
      responseAs[Room]
    }

    /*it should "return a room on GET request on path 'rooms/{type}/{id}' " in {
      Get(ROOMS_WITH_TYPE_AND_ID) ~> route ~> check {
        responseAs[Room]
      }

       it should "respond with a list of available rooms on PUT request on path 'rooms/{type}' " in {
      makeRequestWithEmptyFilter(HttpMethods.PUT)(ROOMS_WITH_TYPE) ~> route ~> check {
        responseAs[Seq[Room]]
      }
    }

      it should "create a new room and respond with that room after PUT request if no room exists with the given type  " in {
      makeRequestWithEmptyFilter(HttpMethods.PUT)(ROOMS_WITH_TYPE) ~> route ~> check {
        responseAs[Seq[Room]] should have size 1
      }

      //ensure creation
      makeRequestWithEmptyFilter(HttpMethods.GET)(ROOMS_WITH_TYPE) ~> route ~> check {
        responseAs[Seq[Room]] should have size 1
      }
      }
    }*/
  }


}
