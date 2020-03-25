package server

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContextExecutor

/*
GET  /rooms

GET  /rooms/{type}         payload -> filtro sui metadati

GET  /rooms/{type}/:id

PUT  /rooms/{type}         payload -> metadati

POST /rooms/{type}         payload -> metadati
*/
class RouteServiceSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest {

  private val ROOMS = "/" + RouteService.ROOMS_PATH
  private val ROOMS_WITH_TYPE = ROOMS + "/" + "type"
  private val ROOMS_WITH_TYPE_AND_ID = ROOMS + "/" + "type" + "/" + "0"

  private implicit val execContext: ExecutionContextExecutor = system.dispatcher
  private val route = RouteService().route

  behavior of "Route Service"

  /// --- Rooms routes --- ///
  it should "handle GET request on path 'rooms'" in {
    Get(ROOMS) ~> route ~> check {
      assert(status.isSuccess)
    }
  }

  it should "handle GET request on path 'rooms/{type}' " in {
    Get(ROOMS_WITH_TYPE) ~> route ~> check {
      assert(status.isSuccess)
    }
  }

  it should "handle PUT request on path 'rooms/{type}' " in {
    Put(ROOMS_WITH_TYPE) ~> route ~> check {
      assert(status.isSuccess)
    }
  }

  it should "handle POST request on path 'rooms/{type}' " in {
    Post(ROOMS_WITH_TYPE) ~> route ~> check {
      assert(status.isSuccess)
    }
  }

  it should "handle GET request on path 'rooms/{type}/{id}' " in {
    Get(ROOMS_WITH_TYPE_AND_ID) ~> route ~> check {
      assert(status.isSuccess)
    }
  }


}