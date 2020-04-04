package common

import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import common.BasicRoomPropertyValueConversions._
import spray.json.JsValue

class JsonSerializationSpec extends AnyFlatSpec with Matchers with BeforeAndAfter with RoomJsonSupport {

  behavior of "room property values"

  "Integer room property values" must "be correctly JSON encoded and decoded" in {
    val testInt: IntRoomPropertyValue = 1
    checkJsonEncoding(testInt, intRoomPropertyJsonFormat write, intRoomPropertyJsonFormat read)
  }

  "String room property values" must "be correctly JSON encoded and decoded" in {
    val testString: StringRoomPropertyValue = "abc"
    checkJsonEncoding(testString, stringRoomPropertyJsonFormat write, stringRoomPropertyJsonFormat read)
  }

  "Boolean room property values" must "be correctly JSON encoded and decoded" in {
    val testBool: BooleanRoomPropertyValue = true
    checkJsonEncoding(testBool, booleanRoomPropertyJsonFormat write, booleanRoomPropertyJsonFormat read)
  }

  behavior of "room property"

  "Room property with int values" must "be correctly JSON encoded and decoded" in {
    val intProp = RoomProperty("A", 1)
    checkJsonEncoding(intProp, roomPropertyJsonFormat write, roomPropertyJsonFormat read)
  }

  "Room property with string values" must "be correctly JSON encoded and decoded" in {
    val stringProp = RoomProperty("A", "abc")
    checkJsonEncoding(stringProp, roomPropertyJsonFormat write, roomPropertyJsonFormat read)
  }

  "Room property with boolean values" must "be correctly JSON encoded and decoded" in {
    val boolProp = RoomProperty("A", true)
    checkJsonEncoding(boolProp, roomPropertyJsonFormat write, roomPropertyJsonFormat read)
  }

  behavior of "filter strategy"

  "Equal strategy" must "be correctly JSON encoded and decoded" in {
    checkJsonEncoding(EqualStrategy(), equalStrategyJsonFormat write, equalStrategyJsonFormat read)
  }

  "Not equal strategy" must "be correctly JSON encoded and decoded" in {
    checkJsonEncoding(NotEqualStrategy(), notEqualStrategyJsonFormat write, notEqualStrategyJsonFormat read)
  }

  "Greater strategy" must "be correctly JSON encoded and decoded" in {
    checkJsonEncoding(GreaterStrategy(), greaterStrategyJsonFormat write, greaterStrategyJsonFormat read)
  }

  "Lower strategy" must "be correctly JSON encoded and decoded" in {
    checkJsonEncoding(LowerStrategy(), lowerStrategyJsonFormat write, lowerStrategyJsonFormat read)
  }

  behavior of "filter options"

  "A single filter option" must "be correctly JSON encoded and decoded" in {
    val p1 = RoomProperty("A", 3) > 1
    checkJsonEncoding(p1, filterOptionJsonFormat write, filterOptionJsonFormat read)
    val p2 = RoomProperty("A", "abc") =!= "abc"
    checkJsonEncoding(p2, filterOptionJsonFormat write, filterOptionJsonFormat read)
    val p3 = RoomProperty("A", false) =:= false
    checkJsonEncoding(p3, filterOptionJsonFormat write, filterOptionJsonFormat read)
  }

  "An epty filter" must "be correctly JSON encoded and decoded" in {
    val empty = FilterOptions.empty()
    checkJsonEncoding(empty, filterOptionsJsonFormat write, filterOptionsJsonFormat read)
  }

  "A filter with just one item" must "be correctly JSON encoded and decoded" in {
    val just = FilterOptions just RoomProperty("A", 1) < 2
    checkJsonEncoding(just, filterOptionsJsonFormat write, filterOptionsJsonFormat read)
  }

  "A generic filter" must "be correctly JSON encoded and decoded" in {
    val filter = FilterOptions just RoomProperty("A", 1) < 2 andThen RoomProperty("B", true) =:= true
    checkJsonEncoding(filter, filterOptionsJsonFormat write, filterOptionsJsonFormat read)
  }

  private def checkJsonEncoding[T](value: T, encode: T => JsValue, decode: JsValue => T) = {
    val encoded = encode(value)
    val decoded = decode(encoded)
    decoded shouldEqual value
  }
}
