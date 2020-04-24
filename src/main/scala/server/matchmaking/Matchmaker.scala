package server.matchmaking

import server.matchmaking.Group.GroupId
import server.room.Client

object Group {
  type GroupId = Int // Must be serializable
}

trait Matchmaker {
  /**
   * It tries to create a fair group of clients from the waiting ones.
   * @return An optional containing each chosen client and its assigned group; If the group could not be created it
   *         returns an empty optional
   */
  def createFairGroup(waitingClients: Map[Client, Any]): Option[Map[Client, GroupId]]
}

object Matchmaker {
  /**
   * It creates a matchmaker with default logic, i.e. it assigns waiting clients just looking to number of required groups
   * and cardinality of each group.
   * @param groups metadata of groups having, for each group, the group Id and its size
   * @return A matchmaker that handles such matchmaking strategy
   */
  def defaultMatchmaker(groups: Map[GroupId, Int]): Matchmaker = DefaultMatchmaker(groups)
}

private case class DefaultMatchmaker(groupsMetadata: Map[GroupId, Int]) extends Matchmaker {

  override def createFairGroup(waitingClients: Map[Client, Any]): Option[Map[Client, GroupId]] =
    if (waitingClients.size > groupsMetadata.keys.sum) {
      val clientIterator = waitingClients.keys.iterator
      val groups = groupsMetadata
        .toSeq
        .flatMap(group => Seq.fill(group._2)((group._1, Client.placeholder()))) // Create a list of available slots
        .map(slot => (clientIterator.next, slot._1)) // Fill each slot with a waiting client
        .toMap
      Option(groups)
    } else {
      Option.empty
    }
}