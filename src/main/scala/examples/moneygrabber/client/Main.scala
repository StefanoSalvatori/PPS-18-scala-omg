package examples.moneygrabber.client


import client.Client
import common.room.FilterOptions
import examples.moneygrabber.client.controller.GameViewController
import examples.moneygrabber.client.view.GameView

import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}

object Main extends App  {
  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global
  private val Host = "localhost"
  private val Port = 8080
  private val client = Client(Host, Port)

  import scala.concurrent.duration._
  val room = Await.result(client.joinOrCreate("game",FilterOptions.empty, Set.empty), 10 seconds)
  val view = new GameView((30,30))
  val controller = new GameViewController(view, room)
  view.show()

}
