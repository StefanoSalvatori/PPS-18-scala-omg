package examples.moneygrabber.client.view.utils

import java.awt.Color

object Utils {

  implicit class ColorWithName(color: Color) {
    def name: String = color match {
      case Color.RED => "Red"
      case Color.BLUE => "Blue"
      case Color.GREEN => "Green"
      case Color.ORANGE => "Orange"
      case Color.PINK => "Pink"
      case Color.WHITE => "White"
      case _ => color.toString
    }
  }

}
