package scalaomg.examples.roll_the_dice.client

import scalaomg.examples.roll_the_dice.common.{MatchState, Team, Turn}

trait PubSubMessage
case class PubSubRoomState(state: MatchState) extends PubSubMessage
case class PubSubSetupGame(assignedTurn: Turn, startingState: MatchState, goalPoints: Int) extends PubSubMessage
case class PubSubNextTurn(turn: Turn) extends PubSubMessage
case class PubSubWin(team: Team) extends PubSubMessage
case object LeavedMatchmaking extends PubSubMessage

trait Subscriber {

  def subscribe(): Unit = PubSub.subscribe(this)

  def onItemPublished(message: PubSubMessage): Unit
}

trait Publisher {

  def publish(item: PubSubMessage): Unit = PubSub.publish(item)
}

object PubSub {

  private var subscriptions: Seq[Subscriber] = Seq.empty

  def subscribe(s: Subscriber): Unit = subscriptions = subscriptions :+ s

  def publish(item: PubSubMessage): Unit = subscriptions.foreach(s => s onItemPublished item)
}
