package scalaomg.examples.moneygrabber.client.controller

import scalaomg.client.core.Client
import scalaomg.common.room.{FilterOptions, RoomProperty}
import scalaomg.examples.moneygrabber.client.view.game.{GameFrame, GameView}
import scalaomg.examples.moneygrabber.client.view.menu.MainMenu
import scalaomg.examples.moneygrabber.client.view.menu.MainMenu.{FourPlayers, Quit, TwoPlayers}
import scalaomg.examples.moneygrabber.common.GameModes
import scalaomg.examples.moneygrabber.common.GameModes.GameMode
import scalaomg.examples.moneygrabber.server.ServerInfo

import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}
import scala.swing.Publisher

class MainMenuController(private val menu: MainMenu,
                         private val serverInfo: ServerInfo) extends Publisher {
  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global
  private val client = Client(serverInfo.host, serverInfo.port)

  listenTo(MainMenu.MenuButtons: _*)
  reactions += {
    case TwoPlayers => startGame(GameModes.Max2)
    case FourPlayers => startGame(GameModes.Max4)
    case Quit => System.exit(0)
  }

  private def startGame(mode: GameMode): Unit = {
    import scalaomg.common.room.RoomPropertyValue.Conversions._

    import scala.concurrent.duration._
    val gameMode = RoomProperty("mode", mode.name)
    val gameStarted = RoomProperty("gameStarted", false)
    val filters = FilterOptions just gameMode =:= mode.name and gameStarted =:= false
    val room = Await.result(client.joinOrCreate("game", filters, Set(gameMode)), 10 seconds)
    val size = room.propertyValues("boardSize").asInstanceOf[Int]
    val view = new GameView((size, size), mode.numPlayers)
    new GameViewController(GameFrame(view), room).openGameView()
  }

}
