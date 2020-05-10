package scalaomg.examples.roll_the_dice.client.view

import java.awt.Dimension

object Utils {

  object DimensionConverters {

    implicit def intTupleToDimension(t: (Int, Int)): Dimension = new Dimension(t._1, t._2)

    implicit def doubleTupleToDimension(t: (Double, Double)): Dimension = new Dimension(t._1.intValue, t._2.intValue)
  }

  import java.awt.Component
  import javax.swing.Box
  def spacing(d: Dimension): Component = Box createRigidArea d
}
