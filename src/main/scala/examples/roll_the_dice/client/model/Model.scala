package examples.roll_the_dice.client.model

import client.Client
import client.room.JoinedRoom
import common.room.FilterOptions
import examples.roll_the_dice.common.{ClientInfo, Turn}
import examples.roll_the_dice.common.MessageDictionary.{DoRoll, StartGame}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait Model {

  def joinGameWithMatchmaking(): Unit

  def leaveMatchmakingQueue(): Unit
}

object Model {
  def apply(): Model = new ModelImpl()
}

class ModelImpl extends Model {

  implicit val executionContext: ExecutionContext = ExecutionContext.global
  import examples.roll_the_dice.common.ServerConfig._
  private val client = Client(host, port)

  val roomName = "matchRoom"

  private var room: JoinedRoom = _
  private var myTurn: Turn = _

  room onMessageReceived {
    case StartGame(assignedTurn) =>
      myTurn = assignedTurn
  }

  override def joinGameWithMatchmaking(): Unit =
    client.matchmaker joinMatchmaking (roomName, ClientInfo()) onComplete { case Success(res) => room = res }

  override def leaveMatchmakingQueue(): Unit = {

  }
}
