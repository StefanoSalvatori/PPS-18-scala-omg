package examples.roll_the_dice.client.model

import client.Client
import client.room.JoinedRoom
import examples.roll_the_dice.client.{PubSubMessage, PubSubNextTurn, PubSubRoomState, PubSubSetupGame, Publisher}
import examples.roll_the_dice.common.{ClientInfo, MatchState, Turn}
import examples.roll_the_dice.common.MessageDictionary.{NextTurn, Roll, StartGame}

import scala.concurrent.ExecutionContext
import scala.util.Success

trait Model {

  def start(): Unit

  def joinGameWithMatchmaking(): Unit

  def leaveMatchmakingQueue(): Unit

  def rollDice(): Unit
}

object Model {
  def apply(): Model = new ModelImpl()

  implicit def stateToPubSubMessage(state: MatchState): PubSubMessage = PubSubRoomState(state)
  implicit def setupGameToPubSubMessage(t: (Turn, MatchState, Int)): PubSubMessage = PubSubSetupGame(t._1, t._2, t._3)
  implicit def nextTurnToPubSubMessage(t: Turn): PubSubMessage = PubSubNextTurn(t)
}

class ModelImpl extends Model with Publisher {

  implicit val executionContext: ExecutionContext = ExecutionContext.global
  import examples.roll_the_dice.common.ServerConfig._
  private val client = Client(host, port)

  val roomName = "matchRoom"

  private var room: JoinedRoom = _

  import Model._
  def start(): Unit = publish(MatchState())

  override def joinGameWithMatchmaking(): Unit =
    client.matchmaker joinMatchmaking (roomName, ClientInfo()) onComplete {
      case Success(res) =>
        room = res

        room onMessageReceived {
          case StartGame(assignedTurn, startingState, goalPoints) =>
            publish((assignedTurn, startingState, goalPoints))
          case NextTurn(turn) =>
            publish(turn)
        }

        room onStateChanged { newState =>
          publish(newState.asInstanceOf[MatchState])
        }
    }

  override def leaveMatchmakingQueue(): Unit = {}

  override def rollDice(): Unit = {
    room send Roll
  }
}
