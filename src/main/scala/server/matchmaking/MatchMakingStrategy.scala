package server.matchmaking

import server.matchmaking.Group.GroupId
import server.room.Client

object Group {
  type GroupId = Int // Must be serializable
}

trait MatchMakingStrategy {

  /**
   * It tries to create a fair group of clients from the waiting ones.
   * @return An optional containing each chosen client and its assigned group; If the group could not be created it
   *         returns an empty optional
   */
  def createFairGroupStrategy: Map[Client, Any] => Option[Map[Client, GroupId]]
}

object MatchMakingStrategy {

  private var groupMetadata: Map[GroupId, Int] = Map()

  def addGroup(id: GroupId, cardinality: Int): MatchMakingStrategy.type = {
    groupMetadata = groupMetadata + (id -> cardinality)
    this
  }

  def addGroups(groups: Map[GroupId, Int]): MatchMakingStrategy.type = {
    groupMetadata = groupMetadata ++ groups
    this
  }
}

case class DefaultMatchMakingStrategy(groupsMetadata: Map[GroupId, Int]) extends MatchMakingStrategy {

  /**
   * It creates groups if enough clients are waiting just looking to number of required groups and cardinality of
   * each group.
   * @return An optional containing each chosen client and its assigned group; If the group could not be created it
   *         returns an empty optional
   */
  override def createFairGroupStrategy: Map[Client, Any] => Option[Map[Client, GroupId]] = waitingClients =>
    if (waitingClients.size > groupsMetadata.keys.sum) {
      val clientIterator = waitingClients.keys.iterator
      val groups = groupsMetadata
        .toSeq
        .flatMap(group => Seq.fill(group._2)((group._1, Client.empty()))) // Create a list of available slots
        .map(slot => (clientIterator.next, slot._1)) // Fill each slot with a waiting client
        .toMap
      Option(groups)
    } else {
      Option.empty
    }
}