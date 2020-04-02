package server

import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import server.room.ServerRoom
import server.route_service.RoomHandler

class RoomHandlerSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest with TestOptions  {

  private val roomHandler: RoomHandler = RoomHandler()
  behavior of "a RoomHandler"

  it should "start with no available rooms" in {
    this.roomHandler.availableRooms should have size 0
  }

  it should "create a new room on createRoom()" in {
    this.roomHandler.defineRoomType(TEST_ROOM_TYPE, ServerRoom(_))
    this.roomHandler.createRoom(TEST_ROOM_TYPE, None)
    this.roomHandler.availableRooms should have size 1
  }

  it should "return room of given type calling getRoomsByType() " in {
    val roomType1 = "type1"
    val roomType2 = "type2"
    this.roomHandler.defineRoomType(roomType1, ServerRoom(_))
    this.roomHandler.defineRoomType(roomType2, ServerRoom(_))
    this.roomHandler.createRoom(roomType1, None)
    this.roomHandler.createRoom(roomType2, None)
    this.roomHandler.createRoom(roomType2, None)

    this.roomHandler.getRoomsByType(roomType2) should have size 2
  }


}
