package examples.roll_the_dice.client.model

import client.Client
import client.room.JoinedRoom
import examples.roll_the_dice.client.{LeavedMatchmaking, PubSubMessage, PubSubNextTurn, PubSubRoomState, PubSubSetupGame, PubSubWin, Publisher}
import examples.roll_the_dice.common.{ClientInfo, MatchState, Team, Turn}
import examples.roll_the_dice.common.MessageDictionary.{NextTurn, Roll, StartGame, Win}

import scala.concurrent.ExecutionContext
import scala.util.Success

trait Model {

  def start(): Unit

  def joinGameWithMatchmaking(desiredTeam: Team): Unit

  def leaveMatchmakingQueue(): Unit

  def rollDice(): Unit
}

object Model {
  def apply(): Model = new ModelImpl()

  implicit def stateToPubSubMessage(state: MatchState): PubSubMessage = PubSubRoomState(state)
  implicit def setupGameToPubSubMessage(d: (Turn, MatchState, Int)): PubSubMessage = PubSubSetupGame(d._1, d._2, d._3)
  implicit def nextTurnToPubSubMessage(t: Turn): PubSubMessage = PubSubNextTurn(t)
  implicit def winTeamToPubSubMessage(t: Team): PubSubMessage = PubSubWin(t)
}

class ModelImpl extends Model with Publisher {

  implicit val executionContext: ExecutionContext = ExecutionContext.global
  import examples.roll_the_dice.common.ServerConfig._
  private val client = Client(host, port)

  val roomName = "matchRoom"

  private var room: JoinedRoom = _

  import Model._
  def start(): Unit = publish(MatchState())

  override def joinGameWithMatchmaking(desiredTeam: Team): Unit =
    client.matchmaker joinMatchmaking (roomName, ClientInfo(desiredTeam)) onComplete {
      case Success(res) =>
        room = res

        room onMessageReceived {
          case StartGame(assignedTurn, startingState, goalPoints) =>
            publish((assignedTurn, startingState, goalPoints))
          case NextTurn(turn) =>
            publish(turn)
          case Win(team) =>
            publish(team)
        }

        room onStateChanged { newState =>
          publish(newState.asInstanceOf[MatchState])
        }

      case _ =>
    }

  override def leaveMatchmakingQueue(): Unit = client.matchmaker leaveMatchmaking roomName onComplete {
    _ => publish(LeavedMatchmaking)
  }

  override def rollDice(): Unit = {
    room send Roll
  }
}
