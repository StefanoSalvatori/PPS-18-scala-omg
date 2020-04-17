package examples.moneygrabber.client.controller

import client.Client
import common.room.{FilterOptions, RoomProperty}
import examples.moneygrabber.client.view.game.{GameFrame, GameView}
import examples.moneygrabber.client.view.menu.MainMenu
import examples.moneygrabber.client.view.menu.MainMenu.{FourPlayers, Quit, TwoPlayers}
import examples.moneygrabber.common.GameModes
import examples.moneygrabber.common.GameModes.GameMode
import examples.moneygrabber.server.{Server, ServerConfig}

import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}
import scala.swing.Publisher

class MainMenuController(private val menu: MainMenu) extends Publisher {
  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global
  private val Host = ServerConfig.ServerHost
  private val Port = ServerConfig.ServerPort
  private val client = Client(Host, Port)

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
    val gameStarted = RoomProperty("gameStarted", false)
    val filters = FilterOptions just gameMode =:= mode.name andThen gameStarted =:= false
    val room = Await.result(client.joinOrCreate("game", filters, Set(gameMode)), 10 seconds)
    val size = room.properties("boardSize").asInstanceOf[Int]
    val view = new GameView((size, size), mode.numPlayers)
    new GameViewController(GameFrame(view), room).openGameView()
  }

}
