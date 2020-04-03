package common

trait FilterStrategy {
  protected def basicStrategy(x: RoomPropertyValue, y: RoomPropertyValue): Int = x compare y.asInstanceOf[x.type]
  def evaluate(x: RoomPropertyValue, y: RoomPropertyValue): Boolean
  def name: String
}

case class EqualStrategy() extends FilterStrategy {
  private val _name = "equal"
  override def evaluate(x: RoomPropertyValue, y: RoomPropertyValue): Boolean = basicStrategy(x, y) == 0
  override def name: String = _name
}

case class NotEqualStrategy() extends FilterStrategy {
  private val _name = "notEqual"
  override def evaluate(x: RoomPropertyValue, y: RoomPropertyValue): Boolean = basicStrategy(x, y) != 0
  override def name: String = _name
}

case class GreaterStrategy() extends FilterStrategy {
  private val _name = "greater"
  override def evaluate(x: RoomPropertyValue, y: RoomPropertyValue): Boolean = basicStrategy(x, y) > 0
  override def name: String = _name
}

case class LowerStrategy() extends FilterStrategy {
  private val _name = "lower"
  override def evaluate(x: RoomPropertyValue, y: RoomPropertyValue): Boolean = basicStrategy(x, y) < 0
  override def name: String = _name
}

trait FilterStrategies { option: RoomProperty =>

  def =:=(that: RoomPropertyValue): FilterOption = filterOption(EqualStrategy(), that)
  def =!=(that: RoomPropertyValue): FilterOption = filterOption(NotEqualStrategy(), that)
  def >(that: RoomPropertyValue): FilterOption= filterOption(GreaterStrategy(), that)
  def <(that: RoomPropertyValue): FilterOption = filterOption(LowerStrategy(), that)

  private def filterOption(filterStrategy: FilterStrategy, that: RoomPropertyValue): FilterOption =
    FilterOption(option.name, filterStrategy, that)
}

case class FilterOption(optionName: String, strategy: FilterStrategy, value: RoomPropertyValue) {
  def andThen(filterOpt: FilterOption): FilterOptions = FilterOptions(Seq(this, filterOpt))
}

object FilterOptions {
  def just(filter: FilterOption): FilterOptions = FilterOptions(Seq(filter))
  def empty(): FilterOptions = FilterOptions(Seq())
}

case class FilterOptions(options: Seq[FilterOption]) {
  def andThen(that: FilterOption): FilterOptions = FilterOptions(options :+ that)
  def ++(that: FilterOptions): FilterOptions = FilterOptions(options ++ that.options)
}
