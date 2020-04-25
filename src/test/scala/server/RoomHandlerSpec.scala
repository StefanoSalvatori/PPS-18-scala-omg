package server

import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestKit
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import server.room.ServerRoom
import common.room.RoomPropertyValueConversions._
import common.room.{FilterOptions, RoomProperty}
import org.scalatest.BeforeAndAfter
import server.routes.RouteCommonTestOptions
import server.utils.ExampleRooms

class RoomHandlerSpec extends AnyFlatSpec
  with Matchers
  with ScalatestRouteTest
  with RouteCommonTestOptions
  with BeforeAndAfter {

  import ExampleRooms.RoomWithProperty
  import ExampleRooms.roomWithPropertyType
  import ExampleRooms.RoomWithProperty2
  import ExampleRooms.roomWithProperty2Type
  import ExampleRooms.LockedRoom
  import ExampleRooms.lockedRoomType

  private var roomHandler: RoomHandler = _

  val roomType1 = "type1"
  val roomType2 = "type2"
  val roomRandomType = "randomType"

  before {
    roomHandler = RoomHandler()
    roomHandler defineRoomType(roomWithPropertyType, RoomWithProperty)
    roomHandler defineRoomType(roomWithProperty2Type, RoomWithProperty2)
    roomHandler defineRoomType (roomType1, () => ServerRoom())
    this.roomHandler.defineRoomType(roomType2, () => ServerRoom())
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  behavior of "a RoomHandler"

  it should "start with no available rooms" in {
    this.roomHandler.availableRooms() should have size 0
  }

  it should "create a new room on createRoom() if the room type is already defined" in {
    this.roomHandler.createRoom(roomType1)
    this.roomHandler.availableRooms() should have size 1
  }

  it should "not create a room on createRoom() if the room type is not defined" in {
    assertThrows[NoSuchElementException] {
      roomHandler createRoom roomRandomType
    }
  }

  it should "return room of given type calling getRoomsByType() " in {
    this.roomHandler.createRoom(roomType1)
    this.roomHandler.createRoom(roomType2)
    this.roomHandler.createRoom(roomType2)
    this.roomHandler.roomsByType(roomType2) should have size 2
  }

  it should "close rooms" in {
    val room = this.roomHandler.createRoom(roomType1)
    this.roomHandler.removeRoom(room.roomId)
    assert(!this.roomHandler.availableRooms().exists(_.roomId == room.roomId))
  }

  it should "not return rooms by type that does not match filters" in {
    roomHandler createRoom roomWithPropertyType
    val property = RoomProperty("a", 0)
    roomHandler.roomsByType(roomWithPropertyType, FilterOptions just property =:= 1) should have size 0
  }

  "An empty filter" should "not affect any room" in {
    roomHandler createRoom roomWithPropertyType
    roomHandler.availableRooms() should have size 1
    val filteredRooms = roomHandler.availableRooms()
    filteredRooms should have size 1
  }

  "If no room can pass the filter, it" should "return an empty set" in {
    roomHandler createRoom roomWithPropertyType
    val testProperty = RoomProperty("a", 0)
    val filter = FilterOptions just testProperty =:= 10
    val filteredRooms = roomHandler.availableRooms(filter)
    filteredRooms should have size 0
  }

  "If a room does not contain a property specified in the filter, such room" should "not be inserted in the result" in {
    val testProperty = RoomProperty("c", true)
    val filter = FilterOptions just testProperty =:= true

    roomHandler createRoom roomWithPropertyType
    val filteredRooms = roomHandler.availableRooms(filter)
    roomHandler.availableRooms() should have size 1
    filteredRooms should have size 0

    roomHandler createRoom roomWithProperty2Type
    val filteredRooms2 = roomHandler.availableRooms(filter)
    roomHandler.availableRooms() should have size 2
    filteredRooms2 should have size 1
  }

  "Correct filter strategies" must "be applied to rooms' properties" in {
    val testProperty = RoomProperty("a", 1)
    val testProperty2 = RoomProperty("b", "a")
    roomHandler createRoom roomWithPropertyType

    val filter = FilterOptions just testProperty < 2 and testProperty2 =:= "abc"
    val filteredRooms = roomHandler.availableRooms(filter)
    filteredRooms should have size 1

    val filter2 = FilterOptions just testProperty > 3
    val filteredRooms2 = roomHandler.availableRooms(filter2)
    filteredRooms2 should have size 0
  }

  it should "filter rooms when return rooms by type" in {
    val testProperty = RoomProperty("a", 1)
    val testProperty2 = RoomProperty("a", 2)

    roomHandler defineRoomType(roomWithPropertyType, RoomWithProperty)
    roomHandler createRoom (roomWithPropertyType, Set(testProperty))
    roomHandler createRoom (roomWithPropertyType, Set(testProperty2))

    val filter = FilterOptions just testProperty =:= 1
    val filteredRooms = roomHandler.roomsByType(roomWithPropertyType, filter)
    filteredRooms should have size 1
  }

  it should "not return locked rooms" in {
    roomHandler defineRoomType (lockedRoomType, LockedRoom)
    roomHandler createRoom lockedRoomType
    roomHandler.availableRooms() should have size 0
    roomHandler defineRoomType (roomWithPropertyType, RoomWithProperty)
    val room = roomHandler createRoom roomWithPropertyType
    val rooms = roomHandler.availableRooms()
    rooms should have size 1
    assert(rooms.map(_ roomId) contains room.roomId)
  }


}
