package scalaomg.examples.roll_the_dice.server.room

import scalaomg.examples.roll_the_dice.common.{A, B, ClientInfo, Team}
import scalaomg.server.matchmaking.Group.GroupId
import scalaomg.server.matchmaking.Matchmaker
import scalaomg.server.room.Client

case class CustomMatchmaker() extends Matchmaker[ClientInfo] {

  override def createFairGroup(waitingClients: Map[Client, ClientInfo]): Option[Map[Client, GroupId]] = {

    // Lazy evaluation: stream combined with headOption let stop when the first admissible grouping is found
    val clientStream = waitingClients.keys.toStream
    val admissibleGroups =
    // Create two groups, each one containing two clients
      for ( // Generate all possible combinations of 4 clients (x,y,z,w)
        clientX <- clientStream; clientY <- clientStream;
        clientZ <- clientStream; clientW <- clientStream
        // Drop combinations where 1 or more clients are the same
        if clientX != clientY && clientX != clientZ && clientX != clientW
        if clientY != clientZ && clientY != clientW
        if clientZ != clientW;
        // Retrieve clients information using their Ids
        x = waitingClients(clientX); y = waitingClients(clientY);
        z = waitingClients(clientZ); w = waitingClients(clientW)
        // Check team desired constraints
        if x.desiredTeam == A && x.desiredTeam == y.desiredTeam
        if z.desiredTeam == B && z.desiredTeam == w.desiredTeam
      ) yield Map(clientX -> 0, clientY -> 0, clientZ -> 1, clientW -> 1)
    admissibleGroups.headOption
  }
}

object CustomMatchmaker {
  implicit def groupIdTeamMapping: Map[GroupId, Team] = Map(0 -> A, 1 -> B)
}
