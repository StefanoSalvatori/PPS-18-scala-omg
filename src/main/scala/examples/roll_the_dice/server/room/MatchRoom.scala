package examples.roll_the_dice.server.room

import  examples.roll_the_dice.common.MessageDictionary._
import examples.roll_the_dice.common.MatchState
import server.room.{Client, ServerRoom, SynchronizedRoomState}

sealed trait Team
case object TeamA extends Team
case object TeamB extends Team

sealed trait Turn
case object A1 extends Turn
case object A2 extends Turn
case object B1 extends Turn
case object B2 extends Turn

case class MatchRoom() extends ServerRoom with SynchronizedRoomState[MatchState] {

  private var clientsTurnMapping = Map.empty[Turn, Client]
  private val turnsOrder = Iterator continually { Seq(A1, B1, A2, B2) } flatten

  private var gameStarted = false
  private var currentTurn: Turn = A1

  private var matchState = MatchState(0, 0)
  private val winPoints = 50

  override def onCreate(): Unit = {}

  override def onClose(): Unit = {}

  override def onJoin(client: Client): Unit = {
    clientsTurnMapping = clientsTurnMapping + (turnsOrder.next -> client)
    if (clientsTurnMapping.size == matchmakingGroups.size) {
      gameStarted = true
      currentTurn = turnsOrder.next
      tell(clientsTurnMapping(currentTurn), Roll)
    }
  }

  override def onLeave(client: Client): Unit = {}

  override def onMessageReceived(client: Client, message: Any): Unit = {
    if (gameStarted && clientsTurnMapping.exists(e => e._1 == currentTurn && e._2 == client)) {
      currentTurn match {
        case A1 | A2 =>
          matchState = matchState addPointsA message.asInstanceOf[DiceResult].value
          if (matchState.pointsA >= winPoints) {
            broadcast("Team A won!")
          } else {
            tell(clientsTurnMapping(turnsOrder.next), Roll)
          }
        case B1 | B2 =>
          matchState = matchState addPointsB message.asInstanceOf[DiceResult].value
          if (matchState.pointsB >= winPoints) {
            broadcast("Team B won!")
          } else {
            tell(clientsTurnMapping(turnsOrder.next), Roll)
          }
      }
    }
  }

  override def currentState: MatchState = matchState
}
