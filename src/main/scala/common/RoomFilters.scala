package common

sealed trait FilterStrategy

trait FilterStrategies[T] {  option: RoomOption[T] =>

  case class EqualStrategy() extends FilterStrategy
  case class NotEqualStrategy() extends FilterStrategy
  case class GreaterStrategy() extends FilterStrategy
  case class LowerStrategy() extends FilterStrategy

  def :=(that: T): FilterOption[T] = filterOption(EqualStrategy(), that)
  def :!=(that: T): FilterOption[T] = filterOption(NotEqualStrategy(), that)
  def >(that: T): FilterOption[T] = filterOption(GreaterStrategy(), that)
  def <(that: T): FilterOption[T] = filterOption(LowerStrategy(), that)

  private def filterOption(filterStrategy: FilterStrategy, that: T): FilterOption[T] =
    FilterOption(option.name, filterStrategy, that)
}

case class FilterOption[T](optName: String, strategy: FilterStrategy, value: T) {
  def andThen(filterOpt: FilterOption[_]): FilterOptions = FilterOptions(Seq(this, filterOpt))
}

object FilterOptions {
  def just(filter: FilterOption[_]): FilterOptions = FilterOptions(Seq(filter))
  def empty(): FilterOptions = FilterOptions(Seq())
}

case class FilterOptions(options: Seq[FilterOption[_]]) {
  def andThen(that: FilterOption[_]): FilterOptions= FilterOptions(options :+ that)
  def ++(that: FilterOptions): FilterOptions = FilterOptions(options ++ that.options)
}
