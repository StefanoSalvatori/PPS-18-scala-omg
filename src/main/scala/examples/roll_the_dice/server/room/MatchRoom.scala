package examples.roll_the_dice.server.room

import examples.roll_the_dice.common.MessageDictionary._
import examples.roll_the_dice.common.{A1, A2, B1, B2, Dice, MatchState, Turn}
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
    println("Joined " + client)
    val currentTurnAssignment = turnsOrder.next
    turnClientMapping = turnClientMapping + (currentTurnAssignment -> client)
    clientTurnMapping = clientTurnMapping + (client -> currentTurnAssignment)
    if (turnClientMapping.size == matchmakingGroups.size) {
      println("Start game")
      gameStarted = true
      turnClientMapping.values.foreach(c => tell(c, StartGame(clientTurnMapping(c), matchState, goalPoints)))
      broadcast(NextTurn(turnsOrder.next))
    }
  }

  override def onMessageReceived(client: Client, message: Any): Unit = {

    println(client.id + " " + message)
    val result = dice.roll()
    clientTurnMapping(client) match {
      case A1 | A2 => matchState = matchState addPointsA result
      case B1 | B2 => matchState = matchState addPointsB result
    }
    
    /*
    if (gameStarted && turnClientMapping.exists(e => e._1 == currentTurn && e._2 == client)) {
      currentTurn match {
        case A1 | A2 =>

          matchState = matchState addPointsA message.asInstanceOf[DiceResult].value
          if (matchState.pointsA >= goalPoints) {
            broadcast("Team A won!")
          } else {
            currentTurn = turnsOrder.next
            tell(turnClientMapping(currentTurn), Roll)
          }
        case B1 | B2 =>
          matchState = matchState addPointsB message.asInstanceOf[DiceResult].value
          if (matchState.pointsB >= goalPoints) {
            broadcast("Team B won!")
          } else {
            currentTurn = turnsOrder.next
            tell(turnClientMapping(currentTurn), Roll)
          }
      }
    }
     */
  }

  override def onClose(): Unit = {}

  override def onLeave(client: Client): Unit = {}

  override def currentState: MatchState = matchState
}
