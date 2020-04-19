package common.room

trait FilterStrategy {
  protected def basicStrategy(x: RoomPropertyValue, y: RoomPropertyValue): Int = x compare y.asInstanceOf[x.type]
  def name: String
  def evaluate(x: RoomPropertyValue, y: RoomPropertyValue): Boolean
}

case class EqualStrategy() extends FilterStrategy {
  override def name: String = "equal"
  override def evaluate(x: RoomPropertyValue, y: RoomPropertyValue): Boolean = basicStrategy(x, y) == 0
}

case class NotEqualStrategy() extends FilterStrategy {
  override def name: String = "notEqual"
  override def evaluate(x: RoomPropertyValue, y: RoomPropertyValue): Boolean = basicStrategy(x, y) != 0
}

case class GreaterStrategy() extends FilterStrategy {
  override def name: String = "greater"
  override def evaluate(x: RoomPropertyValue, y: RoomPropertyValue): Boolean = basicStrategy(x, y) > 0
}

case class LowerStrategy() extends FilterStrategy {
  override def name: String = "lower"
  override def evaluate(x: RoomPropertyValue, y: RoomPropertyValue): Boolean = basicStrategy(x, y) < 0
}

trait FilterStrategies { property: RoomProperty =>

  def =:=(that: RoomPropertyValue): FilterOption = createFilterOption(EqualStrategy(), that)
  def =!=(that: RoomPropertyValue): FilterOption = createFilterOption(NotEqualStrategy(), that)
  def >(that: RoomPropertyValue): FilterOption = createFilterOption(GreaterStrategy(), that)
  def <(that: RoomPropertyValue): FilterOption = createFilterOption(LowerStrategy(), that)

  private def createFilterOption(filterStrategy: FilterStrategy, that: RoomPropertyValue): FilterOption =
    FilterOption(property.name, filterStrategy, that)
}

case class FilterOption(optionName: String, strategy: FilterStrategy, value: RoomPropertyValue) {
  def and(filterOpt: FilterOption): FilterOptions = FilterOptions(Seq(this, filterOpt))
}

object FilterOptions {
  def just(filter: FilterOption): FilterOptions = FilterOptions(Seq(filter))
  def empty: FilterOptions = FilterOptions(Seq.empty[FilterOption])
}

case class FilterOptions(options: Seq[FilterOption]) {
  def and(that: FilterOption): FilterOptions = FilterOptions(options :+ that)
  def ++(that: FilterOptions): FilterOptions = FilterOptions(options ++ that.options)
}
