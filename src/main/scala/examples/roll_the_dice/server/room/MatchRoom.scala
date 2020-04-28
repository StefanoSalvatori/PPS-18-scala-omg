package examples.roll_the_dice.server.room

import examples.roll_the_dice.common.MessageDictionary._
import examples.roll_the_dice.common.{A, A1, A2, B, B1, B2, Dice, MatchState, Team, Turn}
import server.room.{Client, ServerRoom, SynchronizedRoomState}

case class MatchRoom() extends ServerRoom with SynchronizedRoomState[MatchState] {

  private var gameStarted = false

  private var turnClientMapping = Map.empty[Turn, Client]
  private var clientTurnMapping = Map.empty[Client, Turn]

  private val turnsOrder = Iterator continually { Seq(A1, B1) } flatten // , A2, B2
  private var currentTurn: Turn = A1

  private var matchState = MatchState()
  private val goalPoints = 50
  private val dice = Dice.classic()

  override def onCreate(): Unit = this.startStateSynchronization()

  override def onJoin(client: Client): Unit = {

    println(s"Joined ${client.id}")

    // Assign a turn to the client that joined
    val currentTurnAssignment = turnsOrder.next
    turnClientMapping = turnClientMapping + (currentTurnAssignment -> client)
    clientTurnMapping = clientTurnMapping + (client -> currentTurnAssignment)

    // Start the game if all clients connected
    if (turnClientMapping.size == matchmakingGroups.size) {

      println("Start game")

      gameStarted = true
      clientTurnMapping.keys.foreach(c => tell(c, StartGame(clientTurnMapping(c), matchState, goalPoints)))
      broadcast(NextTurn(turnsOrder.next))
    }
  }

  override def onMessageReceived(client: Client, message: Any): Unit =
    if (gameStarted && clientTurnMapping(client) == currentTurn) {

      def handleGameWinning(team: Team): Unit = {
        if (team match {
          case A => matchState.pointsA >= goalPoints
          case B => matchState.pointsB >= goalPoints
        }) {
          gameStarted = false
          broadcast(Win(team))
        }
      }

      println(s"Accepted $message from ${client.id}")

      // Roll the dice, update team points and check for winning
      val result = dice.roll()
      clientTurnMapping(client) match {
        case A1 | A2 =>
          matchState = matchState addPointsA result
          handleGameWinning(A)
        case B1 | B2 =>
          matchState = matchState addPointsB result
          handleGameWinning(B)
      }

      // Proceed with next turn if the game is still going on (anyone din't win)
      if (gameStarted) {
        currentTurn = turnsOrder.next
        clientTurnMapping.keys.foreach(tell(_, NextTurn(currentTurn)))
      }
  }

  override def onClose(): Unit = {}

  override def onLeave(client: Client): Unit = {}

  override def currentState: MatchState = matchState
}
