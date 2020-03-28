package common

trait FilterStrategy
case class EqualStrategy() extends FilterStrategy
case class NotEqualStrategy() extends FilterStrategy
case class GreaterStrategy() extends FilterStrategy
case class LowerStrategy() extends FilterStrategy

trait FilterStrategies[T] {  opt: MyOpt[T] =>

  def :=(that: T): FilterOption[T] = filterOption(EqualStrategy(), that)
  def :!=(that: T): FilterOption[T] = filterOption(NotEqualStrategy(), that)
  def >(that: T): FilterOption[T] = filterOption(GreaterStrategy(), that)
  def <(that: T): FilterOption[T] = filterOption(LowerStrategy(), that)

  private def filterOption(filterStrategy: FilterStrategy, that: T): FilterOption[T] =
    FilterOption(opt.name, filterStrategy, that)
}

case class FilterOption[T](optName: String, strategy: FilterStrategy, value: T) {
  def andThen[U](filterOpt: FilterOption[U]): FilterOptions[_] = new FilterOptions(Seq(this, filterOpt))
}

object FilterOptions {
  def just[T](filter: FilterOption[T]): FilterOptions[_] = new FilterOptions(Seq(filter))
}

class FilterOptions[_](val options: Seq[FilterOption[_]]) {
  def andThen[T](filterOpt: FilterOption[T]): FilterOptions[_]= new FilterOptions(options ++ Seq(filterOpt))

}

object MyOpt {
  def apply[T](name: String, value: T): MyOpt[T] = new MyOpt(name, value)
}

case class MyOpt[T](name: String, value: T) extends FilterStrategies[T]

