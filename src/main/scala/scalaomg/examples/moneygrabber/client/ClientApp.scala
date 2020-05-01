package scalaomg.examples.moneygrabber.client


import scalaomg.examples.moneygrabber.client.controller.MainMenuController
import scalaomg.examples.moneygrabber.client.view.menu.MainMenu

object ClientApp extends App {
  val mainWindow = new MainMenu()
  val mainController = new MainMenuController(mainWindow)
  mainWindow.pack()
  mainWindow.centerOnScreen()
  mainWindow.open()
}
