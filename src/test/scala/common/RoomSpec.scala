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

  val testRoom: Room = new Room {
    override val roomId: RoomId = "id"
    var a: Int = valueA
    var b: String = valueB
    var c: Boolean = valueC
  }

  behavior of "concept of room shared between client and server"

  "A room" must "return correct values of its properties" in {
    testRoom valueOf nameA shouldEqual valueA
    testRoom valueOf nameB shouldEqual valueB
    testRoom valueOf nameC shouldEqual valueC
  }


  "A room " must "return its room property value when required" in {
    assert((testRoom `valueOf~AsProperty` nameA).isInstanceOf[RoomPropertyValue])
  }

  "An empty set of properties" must "not update any property" in {
    val p = Set.empty[RoomProperty]
    testRoom setProperties p
    testRoom valueOf nameA shouldEqual valueA
    testRoom valueOf nameB shouldEqual valueB
    testRoom valueOf nameC shouldEqual valueC
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
