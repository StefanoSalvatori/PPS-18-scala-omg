package client

import common._
import common.BasicRoomPropertyValueConversions._

import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RoomFilterSpec extends AnyFlatSpec
  with Matchers
  with BeforeAndAfter {

  private val testPropertyName = "A"
  private val testPropertyValue = 1
  private val testProperty: RoomProperty = RoomProperty(testPropertyName, testPropertyValue)
  private val testPropertyName2 = "B"
  private val testPropertyValue2 = "abc"
  private val testProperty2: RoomProperty = RoomProperty(testPropertyName2, testPropertyValue2)
  private val testPropertyName3 = "C"
  private val testPropertyValue3 = true
  private val testProperty3: RoomProperty = RoomProperty(testPropertyName3, testPropertyValue3)

  case class MyRoomPropertyValue(a: String, b: Int) extends RoomPropertyValue {
    override def compare(that: this.type): Int = this.b - that.b
  }
  private val myRoomPropertyName = "D"
  private val myProperty = RoomProperty(myRoomPropertyName, MyRoomPropertyValue("abc", 1))
  private val myTestProperty = MyRoomPropertyValue("cde", 3)
  private val myTestProperty2 = MyRoomPropertyValue("abc", 1)

  behavior of "RoomFilters"

  "A filter option" should "contain property name, filter strategy and filter value" in {
    val filterOption = testProperty =!= 2
    checkFilterOptionCorrectness(filterOption)(testProperty.name, NotEqualStrategy(), 2)
  }

  "An empty filter" should "have no elements" in {
    val empty = FilterOptions.empty()
    assert(empty.options.isEmpty)
  }

  "A filter created using just" should "have only the specified element" in {
    val just = FilterOptions just testProperty > 1
    just.options should have size 1
    checkFilterOptionCorrectness(just.options.head)(testProperty.name, GreaterStrategy(), 1)
  }

  "A concatenation of filter clauses " should "create a filter with all such clauses" in {
    val filter = testProperty =!= 1 andThen testProperty2 =:= "aba" andThen testProperty3 =:= true
    val options = filter.options
    options should have size 3
    checkFilterOptionCorrectness(options.head)(testProperty.name, NotEqualStrategy(), 1)
    checkFilterOptionCorrectness(options(1))(testProperty2.name, EqualStrategy(), "aba")
    checkFilterOptionCorrectness(options(2))(testProperty3.name, EqualStrategy(), true)
  }

  "A custom filter created using just" should "contain just the specified element" in {
    val just = FilterOptions just myProperty > MyRoomPropertyValue("cde", 3)
    just.options should have size 1
    checkFilterOptionCorrectness(just.options.head)(myProperty.name, GreaterStrategy(), MyRoomPropertyValue("cde", 3))
  }

  "A concatenation of custom filter clauses " should "create a filter with all such clauses" in {

    val filter = FilterOptions just myProperty > myTestProperty andThen myProperty =:= myTestProperty2
    val options = filter.options
    options should have size 2
    checkFilterOptionCorrectness(options.head)(myProperty.name, GreaterStrategy(), myTestProperty)
    checkFilterOptionCorrectness(options(1))(myProperty.name, EqualStrategy(), myTestProperty2)
  }

  "Filters" should "indifferently mix simple and custom values" in {
    val filter = FilterOptions just myProperty =:= myTestProperty andThen testProperty =!= 2
    val options = filter.options
    options should have size 2
    checkFilterOptionCorrectness(options.head)(myProperty.name,EqualStrategy(), myTestProperty)
    checkFilterOptionCorrectness(options(1))(testProperty.name, NotEqualStrategy(), 2)
  }

  private def checkFilterOptionCorrectness(option: FilterOption)(propertyName: String, strategy: FilterStrategy, value: RoomPropertyValue): Unit = {
    option.optionName shouldEqual propertyName
    option.strategy shouldEqual strategy
    option.value shouldEqual value
  }
}
