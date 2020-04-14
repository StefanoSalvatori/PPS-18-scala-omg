package examples.moneygrabber.client.view

import java.awt.Color


object Utils {

  implicit class ColorWithName(color: Color) {
    def name: String = color match {
      case Color.RED => "Red"
      case Color.BLUE => "Blue"
      case Color.GREEN => "Green"
      case Color.ORANGE => "Orange"
      case Color.WHITE => "White"
      case _ => color.toString
    }
  }

}
