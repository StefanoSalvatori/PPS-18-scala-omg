package examples.roll_the_dice.client.controller

object Main extends App {

  Controller().start()
}

import examples.roll_the_dice.client.model.Model
import examples.roll_the_dice.client.view.View

case class Controller() {

  private val model = Model()
  private val view = View(this)

  def start(): Unit = view.start()
}
