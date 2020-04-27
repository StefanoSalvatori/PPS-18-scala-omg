package examples.roll_the_dice.client.model

import client.Client
import client.room.JoinedRoom
import examples.roll_the_dice.client.{PubSubRoomState, Publisher}
import examples.roll_the_dice.common.{ClientInfo, MatchState, Turn}
import examples.roll_the_dice.common.MessageDictionary.StartGame

import scala.concurrent.ExecutionContext
import scala.util.Success

trait Model {

  def start(): Unit

  def joinGameWithMatchmaking(): Unit

  def leaveMatchmakingQueue(): Unit
}

object Model {
  def apply(): Model = new ModelImpl()

  implicit def stateToPubSubMessage(state: MatchState): PubSubRoomState = PubSubRoomState(state)
}

class ModelImpl extends Model with Publisher {

  implicit val executionContext: ExecutionContext = ExecutionContext.global
  import examples.roll_the_dice.common.ServerConfig._
  private val client = Client(host, port)

  val roomName = "matchRoom"

  private var room: JoinedRoom = _
  private var myTurn: Turn = _

  import Model.stateToPubSubMessage
  def start(): Unit = publish(MatchState())

  override def joinGameWithMatchmaking(): Unit =
    client.matchmaker joinMatchmaking (roomName, ClientInfo()) onComplete {
      case Success(res) =>
        room = res

        room onMessageReceived {
          case StartGame(assignedTurn) =>
            myTurn = assignedTurn
        }

        room onStateChanged { newState =>
          publish(newState.asInstanceOf[MatchState])
        }
    }

  override def leaveMatchmakingQueue(): Unit = {}
}
