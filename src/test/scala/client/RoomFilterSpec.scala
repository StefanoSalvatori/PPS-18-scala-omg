package client

import common._
import common.BasicRoomPropertyValueConversions._
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RoomFilterSpec extends AnyFlatSpec with Matchers with BeforeAndAfter {

  private val testPropertyName = "A"
  private val testPropertyValue = 1
  private val testProperty: RoomProperty = RoomProperty(testPropertyName, testPropertyValue)
  private val testPropertyName2 = "B"
  private val testPropertyValue2 = "abc"
  private val testProperty2: RoomProperty = RoomProperty(testPropertyName2, testPropertyValue2)
  private val testPropertyName3 = "C"
  private val testPropertyValue3 = true
  private val testProperty3: RoomProperty = RoomProperty(testPropertyName3, testPropertyValue3)

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

  private def checkFilterOptionCorrectness(option: FilterOption)(propertyName: String, strategy: FilterStrategy, value: RoomPropertyValue): Unit = {
    option.optionName shouldEqual propertyName
    option.strategy shouldEqual strategy
    option.value shouldEqual value
  }
}
