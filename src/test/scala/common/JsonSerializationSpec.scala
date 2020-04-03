package common

import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import common.BasicRoomPropertyValueConversions._
import spray.json.JsValue

class JsonSerializationSpec extends AnyFlatSpec with Matchers with BeforeAndAfter with RoomJsonSupport {

  behavior of "room property values"

  "Integer room property values" must "be JSON encoded and decoded correctly" in {
    val testInt: IntRoomPropertyValue = 1
    checkJsonEncoding(testInt, intRoomPropertyJsonFormat write, intRoomPropertyJsonFormat read)
  }

  "String room property values" must "be JSON encoded and decoded correctly" in {
    val testString: StringRoomPropertyValue = "abc"
    checkJsonEncoding(testString, stringRoomPropertyJsonFormat write, stringRoomPropertyJsonFormat read)
  }

  "Boolean room property values" must "be JSON encoded and decoded correctly" in {
    val testBool: BooleanRoomPropertyValue = true
    checkJsonEncoding(testBool, booleanRoomPropertyJsonFormat write, booleanRoomPropertyJsonFormat read)
  }

  behavior of "room property"

  "Room property with int values" must "be handled correctly" in {
    val intProp = RoomProperty("A", 1)
    checkJsonEncoding(intProp, roomPropertyJsonFormat write, roomPropertyJsonFormat read)
  }

  "Room property with string values" must "be handled correctly" in {
    val stringProp = RoomProperty("A", "abc")
    checkJsonEncoding(stringProp, roomPropertyJsonFormat write, roomPropertyJsonFormat read)
  }

  "Room property with boolean values" must "be handled correctly" in {
    val boolProp = RoomProperty("A", true)
    checkJsonEncoding(boolProp, roomPropertyJsonFormat write, roomPropertyJsonFormat read)
  }

  private def checkJsonEncoding[T](value: T, encode: T => JsValue, decode: JsValue => T) = {
    val encoded = encode(value)
    val decoded = decode(encoded)
    decoded shouldEqual value
  }
}
