package client

import common.room.RoomPropertyValueConversions._
import common.room.{EqualStrategy, FilterOption, FilterOptions, FilterStrategy, GreaterStrategy, NotEqualStrategy, RoomProperty, RoomPropertyValue}
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RoomFilterSpec extends AnyFlatSpec with Matchers with BeforeAndAfter {

  private val intPropertyName = "A"
  private val intPropertyValue = 1
  private val intProperty: RoomProperty = RoomProperty(intPropertyName, intPropertyValue)
  private val stringPropertyName = "B"
  private val stringPropertyValue = "abc"
  private val stringProperty: RoomProperty = RoomProperty(stringPropertyName, stringPropertyValue)
  private val booleanPropertyName = "C"
  private val booleanPropertyValue = true
  private val booleanProperty: RoomProperty = RoomProperty(booleanPropertyName, booleanPropertyValue)

  behavior of "RoomFilters"

  "A filter option" should "contain property name, filter strategy and filter value" in {
    val filterOption = intProperty =!= 2
    checkFilterOptionCorrectness(filterOption)(intProperty.name, NotEqualStrategy(), 2)
  }

  "An empty filter" should "have no elements" in {
    val empty = FilterOptions.empty
    assert(empty.options.isEmpty)
  }

  "A filter created using just" should "have only the specified element" in {
    val just = FilterOptions just intProperty > 1
    just.options should have size 1
    checkFilterOptionCorrectness(just.options.head)(intProperty.name, GreaterStrategy(), 1)
  }

  "A concatenation of filter clauses " should "create a filter with all such clauses" in {
    val filter = intProperty =!= 1 andThen stringProperty =:= "aba" andThen booleanProperty =:= true
    val options = filter.options
    options should have size 3
    checkFilterOptionCorrectness(options.head)(intProperty.name, NotEqualStrategy(), 1)
    checkFilterOptionCorrectness(options(1))(stringProperty.name, EqualStrategy(), "aba")
    checkFilterOptionCorrectness(options(2))(booleanProperty.name, EqualStrategy(), true)
  }

  private def checkFilterOptionCorrectness(option: FilterOption)(propertyName: String, strategy: FilterStrategy, value: RoomPropertyValue): Unit = {
    option.optionName shouldEqual propertyName
    option.strategy shouldEqual strategy
    option.value shouldEqual value
  }
}
