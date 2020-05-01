package scalaomg.examples.moneygrabber.client


import scalaomg.examples.moneygrabber.client.controller.MainMenuController
import scalaomg.examples.moneygrabber.client.view.menu.MainMenu
import scalaomg.examples.moneygrabber.server.ServerInfo

object ClientApp extends App {
  private val Host = if (args.length > 0) args(0) else ServerInfo.DefaultHost
  private val Port = if (args.length > 1) args(1).toInt else ServerInfo.DefaultPort
  val mainWindow = new MainMenu()
  val mainController = new MainMenuController(mainWindow, ServerInfo(Host,Port))
  mainWindow.pack()
  mainWindow.centerOnScreen()
  mainWindow.open()
}
