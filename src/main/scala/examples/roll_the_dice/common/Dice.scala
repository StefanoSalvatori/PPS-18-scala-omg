package examples.roll_the_dice.common

import scala.util.Random

trait Dice {
  /**
   * Roll the dice.
   * @return the result of the dice rolling
   */
  def roll(): Int
}

object Dice {
  def classic(): Dice = DiceImpl(1, 6) //scalastyle:ignore magic.number
  def custom(min: Int, max: Int): Dice = DiceImpl(min, max)
}

case class DiceImpl(private val min: Int, private val max: Int) extends Dice {

  override def roll(): Int = min + Random.nextInt(max - min + 1)
}