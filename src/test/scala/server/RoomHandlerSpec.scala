package server

import akka.http.scaladsl.testkit.ScalatestRouteTest
import common.RoomJsonSupport
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import server.route_service.RoomHandler

class RoomHandlerSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest  {

  private val roomHandler: RoomHandler = RoomHandler()
  behavior of "a RoomHandler"

  it should "start with no available rooms" in {
    this.roomHandler.availableRooms should have size 0
  }


}
