package examples.roll_the_dice.server.room

import examples.roll_the_dice.common.ClientInfo
import server.matchmaking.Group.GroupId
import server.matchmaking.Matchmaker
import server.room.Client

case class CustomMatchmaker() extends Matchmaker[ClientInfo] {

  override def createFairGroup(waitingClients: Map[Client, ClientInfo]): Option[Map[Client, GroupId]] = None
}
