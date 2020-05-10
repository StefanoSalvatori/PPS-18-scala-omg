package scalaomg.examples.roll_the_dice.server.room

import scalaomg.examples.roll_the_dice.common.MessageDictionary._
import scalaomg.examples.roll_the_dice.common.{A, A1, A2, B, B1, B2, Dice, MatchState, Team, Turn}
import scalaomg.server.room.features.SynchronizedRoomState
import scalaomg.server.room.{Client, ServerRoom}

case class MatchRoom() extends ServerRoom with SynchronizedRoomState[MatchState] {

  private var gameStarted = false

  private var joinedA = 0
  private val expectedA = 2
  private var joinedB = 0
  private val expectedB = 2

  private var turnClientMapping = Map.empty[Turn, Client]
  private var clientTurnMapping = Map.empty[Client, Turn]

  private val turnsOrder = Iterator continually { Seq(A1, B1, A2, B2) } flatten
  private var currentTurn: Turn = A1

  private var matchState = MatchState()
  private val goalPoints = 10
  private val dice = Dice.classic()

  override def onCreate(): Unit = this.startStateSynchronization()

  override def onJoin(client: Client): Unit = {

    println(s"Joined ${client.id}")

    // Assign a turn to the client that joined
    var assignedTurn: Turn = A1
    CustomMatchmaker groupIdTeamMapping matchmakingGroups(client) match {
      case A =>
        joinedA = joinedA + 1
        assignedTurn = if (joinedA < expectedA) A1 else A2
      case B =>
        joinedB = joinedB + 1
        assignedTurn = if (joinedB < expectedB) B1 else B2
    }
    turnClientMapping = turnClientMapping + (assignedTurn -> client)
    clientTurnMapping = clientTurnMapping + (client -> assignedTurn)

    println(clientTurnMapping)

    // Start the game if all clients connected
    if (turnClientMapping.size == matchmakingGroups.size) {

      println("Start game")

      gameStarted = true
      clientTurnMapping.foreach(kv => tell(kv._1, StartGame(kv._2, matchState, goalPoints)))
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
          close()
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
        broadcast(NextTurn(currentTurn))
      }
  }

  override def onClose(): Unit = {}

  override def onLeave(client: Client): Unit = {}

  override def currentState: MatchState = matchState
}
