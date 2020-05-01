package scalaomg.examples.roll_the_dice.client.model

import scalaomg.client.core.Client
import scalaomg.client.room.JoinedRoom
import scalaomg.examples.roll_the_dice.client.{LeavedMatchmaking, PubSubMessage, PubSubNextTurn, PubSubRoomState, PubSubSetupGame, PubSubWin, Publisher}
import scalaomg.examples.roll_the_dice.common.{ClientInfo, MatchState, ServerInfo, Team, Turn}
import scalaomg.examples.roll_the_dice.common.MessageDictionary.{NextTurn, Roll, StartGame, Win}

import scala.concurrent.ExecutionContext
import scala.util.Success

trait Model {

  def start(): Unit

  def joinGameWithMatchmaking(desiredTeam: Team): Unit

  def leaveMatchmakingQueue(): Unit

  def rollDice(): Unit
}

object Model {
  def apply(serverInfo: ServerInfo): Model = new ModelImpl(serverInfo)

  implicit def stateToPubSubMessage(state: MatchState): PubSubMessage = PubSubRoomState(state)
  implicit def setupGameToPubSubMessage(d: (Turn, MatchState, Int)): PubSubMessage = PubSubSetupGame(d._1, d._2, d._3)
  implicit def nextTurnToPubSubMessage(t: Turn): PubSubMessage = PubSubNextTurn(t)
  implicit def winTeamToPubSubMessage(t: Team): PubSubMessage = PubSubWin(t)
}

class ModelImpl(serverInfo: ServerInfo) extends Model with Publisher {

  implicit val executionContext: ExecutionContext = ExecutionContext.global
  private val client = Client(serverInfo.host, serverInfo.port)

  val roomName = "matchRoom"

  private var room: JoinedRoom = _

  import Model._
  override def start(): Unit = publish(MatchState())

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
