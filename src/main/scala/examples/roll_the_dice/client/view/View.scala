package examples.roll_the_dice.client.view

import examples.roll_the_dice.client.controller.Controller

trait View {
  def start(): Unit
}

object View {
  def apply(observer: Controller): View = new ViewImpl(observer)
}

class ViewImpl(observer: Controller) extends View {

  override def start(): Unit = {

  }
}
