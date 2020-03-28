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
  def andThen[U](filterOpt: FilterOption[U]): FilterOptions[_] = FilterOptions(Seq(this, filterOpt))
}

object FilterOptions {
  def just(filter: FilterOption[_]): FilterOptions[_] = FilterOptions(Seq(filter))
  def empty(): FilterOptions[_] = FilterOptions(Seq())
}

case class FilterOptions[_](options: Seq[FilterOption[_]]) {
  def andThen[T](that: FilterOption[T]): FilterOptions[_]= FilterOptions(options :+ that)
  def ++[_](that: FilterOptions[_]): FilterOptions[_] = FilterOptions(options ++ that.options)
}
