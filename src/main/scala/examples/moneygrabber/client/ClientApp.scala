package examples.moneygrabber.client


import examples.moneygrabber.client.controller.MainMenuController
import examples.moneygrabber.client.view.menu.MainMenu

object ClientApp extends App {
  val mainWindow = new MainMenu()
  val mainController = new MainMenuController(mainWindow)
  mainWindow.pack()
  mainWindow.centerOnScreen()
  mainWindow.open()
}
