package scalaomg.examples.roll_the_dice.common

sealed trait Team
case object A extends Team
case object B extends Team

sealed trait Turn
case object A1 extends Turn
case object A2 extends Turn
case object B1 extends Turn
case object B2 extends Turn
