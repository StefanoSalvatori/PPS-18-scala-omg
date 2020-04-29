package examples.roll_the_dice.client.view

object WindowSize {

  import java.awt.Toolkit
  val screenWidth: Double =  Toolkit.getDefaultToolkit.getScreenSize.getWidth
  val screenHeight: Double = Toolkit.getDefaultToolkit.getScreenSize.getHeight

  private val windowPercentage = 0.6
  val windowWidth: Double = screenWidth * windowPercentage
  val windowHeight: Double = screenHeight * windowPercentage
}
