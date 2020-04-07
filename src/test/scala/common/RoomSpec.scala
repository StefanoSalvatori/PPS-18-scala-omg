package common

import common.SharedRoom.{Room, RoomId}
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import common.BasicRoomPropertyValueConversions._

class RoomSpec extends AnyFlatSpec with Matchers with BeforeAndAfter {

  val nameA = "a"; val valueA = 1
  val nameB = "b"; val valueB = "abc"
  val nameC = "c"; val valueC = false
  val nameD = "d"; val valueD = 0.1

  val testRoom: Room = new Room {
    override val roomId: RoomId = "id"
    var a: Int = valueA
    var b: String = valueB
    var c: Boolean = valueC
    var d: Double = valueD
  }

  behavior of "concept of room shared between client and server"

  "A room" must "return correct values of its properties" in {
    testRoom valueOf nameA shouldEqual valueA
    testRoom valueOf nameB shouldEqual valueB
    testRoom valueOf nameC shouldEqual valueC
    testRoom valueOf nameD shouldEqual valueD
  }

  "A room" must "return its room property value when required" in {
    assert((testRoom `valueOf~AsProperty` nameA).isInstanceOf[RoomPropertyValue])
  }

  "Given a property name, a room" must "return the associated room property" in {
   testRoom propertyOf nameA shouldEqual RoomProperty(nameA, valueA)
   testRoom propertyOf nameB shouldEqual RoomProperty(nameB, valueB)
   testRoom propertyOf nameC shouldEqual RoomProperty(nameC, valueC)
   testRoom propertyOf nameD shouldEqual RoomProperty(nameD, valueD)
  }

  "An empty set of properties" must "not update any property" in {
    val p = Set.empty[RoomProperty]
    testRoom setProperties p
    testRoom valueOf nameA shouldEqual valueA
    testRoom valueOf nameB shouldEqual valueB
    testRoom valueOf nameC shouldEqual valueC
    testRoom valueOf nameD shouldEqual valueD
  }

  "Room properties" must "be correctly updated" in {
    val p = Set(RoomProperty(nameA, 1), RoomProperty(nameB, "qwe"))
    testRoom setProperties p
    testRoom valueOf nameA shouldEqual 1
    testRoom valueOf nameB shouldEqual "qwe"
  }

  "Not existing property in a given room" should "notify the error when trying to read it" in {
    assertThrows[NoSuchFieldException] {
      testRoom valueOf "randomName"
    }
  }

  "Not existing property in a given room" should "be safely handled when trying to write it" in {
    testRoom setProperties Set(RoomProperty("randomName", 0))
    noException
  }
}
