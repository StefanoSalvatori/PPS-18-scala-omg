package common.room

import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import common.room.RoomPropertyValueConversions._

class RoomPropertySpec extends AnyFlatSpec
    with Matchers
    with BeforeAndAfter {

  val intValue = 0; val intPropertyValue: IntRoomPropertyValue = intValue
  val stringValue = "abc"; val stringPropertyValue: StringRoomPropertyValue = stringValue
  val booleanValue = false; val booleanPropertyValue: BooleanRoomPropertyValue = booleanValue
  val doubleValue = 0.1; val doublePropertyValue: DoubleRoomPropertyValue = doubleValue

  behavior of "Room property values"

  it should "return correct values" in {
    intPropertyValue.value shouldEqual intValue
    stringPropertyValue.value shouldEqual stringValue
    booleanPropertyValue.value shouldEqual booleanValue
    doublePropertyValue.value shouldEqual doubleValue
  }

  it should "correctly transform a property value in the corresponding first class value" in {
    RoomPropertyValue runtimeValue intPropertyValue shouldEqual intValue
    RoomPropertyValue runtimeValue stringPropertyValue shouldEqual stringValue
    RoomPropertyValue runtimeValue booleanPropertyValue shouldEqual booleanValue
    RoomPropertyValue runtimeValue doublePropertyValue shouldEqual doubleValue
  }

  it should "instantiate the correct property value, starting from an unknown type" in {
    val intValue = 1; val intTest: Any = intValue
    RoomPropertyValue valueToRoomPropertyValue intTest shouldEqual IntRoomPropertyValue(intValue)
    val stringValue = "abc"; val stringTest: Any = stringValue
    RoomPropertyValue valueToRoomPropertyValue stringTest shouldEqual StringRoomPropertyValue(stringValue)
    val booleanValue = true; val booleanTest: Any = booleanValue
    RoomPropertyValue valueToRoomPropertyValue booleanTest shouldEqual BooleanRoomPropertyValue(booleanValue)
    val doubleValue = 0.1; val doubleTest: Any = doubleValue
    RoomPropertyValue valueToRoomPropertyValue doubleTest shouldEqual DoubleRoomPropertyValue(doubleValue)
  }
}
