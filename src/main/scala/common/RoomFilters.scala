package common

abstract class FilterStrategy
case class EqualStrategy() extends FilterStrategy
case class NotEqualStrategy() extends FilterStrategy
case class GreaterStrategy() extends FilterStrategy
case class LowerStrategy() extends FilterStrategy

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
