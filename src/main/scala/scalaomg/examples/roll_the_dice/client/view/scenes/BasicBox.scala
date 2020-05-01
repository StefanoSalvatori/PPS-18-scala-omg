package scalaomg.examples.roll_the_dice.client.view.scenes

import javax.swing.Box

sealed trait BoxType
case object VerticalBox extends BoxType
case object HorizontalBox extends BoxType

trait BasicBox {

  private val _box = boxType match {
    case VerticalBox => Box.createVerticalBox()
    case HorizontalBox => Box.createHorizontalBox()
  }

  def boxType: BoxType

  def box: Box = _box
}
