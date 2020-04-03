package server

import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import server.room.ServerRoom
import server.routes.RouteCommonTestOptions

class RoomHandlerSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest with RouteCommonTestOptions  {

  private val roomHandler: RoomHandler = RoomHandler(system)
  behavior of "a RoomHandler"

  it should "start with no available rooms" in {
    this.roomHandler.getAvailableRooms() should have size 0
  }

  it should "return an empty set if the given type is not defined" in {
    this.roomHandler.getRoomsByType("type") shouldBe empty
  }



  it should "create a new room" in {
    this.roomHandler.defineRoomType(TEST_ROOM_TYPE, ServerRoom(_))
    this.roomHandler.createRoom(TEST_ROOM_TYPE, Set.empty)
    this.roomHandler.getAvailableRooms() should have size 1
  }

  it should "return room of given type " in {
    val roomType1 = "type1"
    val roomType2 = "type2"
    this.roomHandler.defineRoomType(roomType1, ServerRoom(_))
    this.roomHandler.defineRoomType(roomType2, ServerRoom(_))
    this.roomHandler.createRoom(roomType1, Set.empty)
    this.roomHandler.createRoom(roomType2, Set.empty)
    this.roomHandler.createRoom(roomType2, Set.empty)

    this.roomHandler.getRoomsByType(roomType2) should have size 2
    this.roomHandler.getRoomsByType(roomType1) should have size 1
  }

  it should "return a room given type and id" in {
    val roomType1 = "type1"
    val roomType2 = "type2"
    this.roomHandler.defineRoomType(roomType1, ServerRoom(_))
    this.roomHandler.defineRoomType(roomType2, ServerRoom(_))
    val room1 = this.roomHandler.createRoom(roomType1, Set.empty)
    this.roomHandler.createRoom(roomType2, Set.empty)
    this.roomHandler.createRoom(roomType2, Set.empty)

    val roomOpt = this.roomHandler.getRoomByTypeAndId(roomType1, room1.roomId)
    assert(roomOpt.isDefined)
    assert(room1.roomId == roomOpt.get.roomId)
  }

  it should "return empty if the id does not match" in {
    val roomType1 = "type1"
    val roomType2 = "type2"
    this.roomHandler.defineRoomType(roomType1, ServerRoom(_))
    this.roomHandler.defineRoomType(roomType2, ServerRoom(_))
    val room1 = this.roomHandler.createRoom(roomType1, Set.empty)
    this.roomHandler.createRoom(roomType2, Set.empty)
    this.roomHandler.createRoom(roomType2, Set.empty)

    val roomOpt = this.roomHandler.getRoomByTypeAndId(roomType1, "3")
    assert(roomOpt.isEmpty)
  }


}
