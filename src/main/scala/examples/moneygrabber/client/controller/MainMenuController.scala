package examples.moneygrabber.client.controller

import client.{Client, ClientImpl}
import common.room.{FilterOptions, RoomProperty}
import examples.moneygrabber.client.view.game.{GameFrame, GameView}
import examples.moneygrabber.client.view.menu.MainMenu
import examples.moneygrabber.client.view.menu.MainMenu.{FourPlayers, Quit, TwoPlayers}
import examples.moneygrabber.common.GameModes
import examples.moneygrabber.common.GameModes.GameMode

import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}
import scala.swing.Publisher

class MainMenuController(private val menu: MainMenu) extends Publisher {
  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global
  private val Host = "localhost"
  private val Port = 8080
  private val client: ClientImpl = Client(Host, Port)

  listenTo(MainMenu.MenuButtons: _*)
  reactions += {
    case TwoPlayers => startGame(GameModes.Max2)
    case FourPlayers => startGame(GameModes.Max4)
    case Quit => System.exit(0)
  }

  private def startGame(mode: GameMode): Unit = {
    import common.room.RoomPropertyValueConversions._

    import scala.concurrent.duration._
    val gameMode = RoomProperty("mode", mode.name)
    val filters = FilterOptions just gameMode =:= mode.name
    val room = Await.result(client.joinOrCreate("game", filters, Set(gameMode)), 10 seconds)
    val size = room.properties("size").asInstanceOf[Int]
    val view = new GameView((size, size), mode.numPlayers)
    new GameViewController(GameFrame(view), room).openGameView()
  }

}
