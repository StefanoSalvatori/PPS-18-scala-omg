package server.examples
import server.matchmaking.Group.GroupId
import server.room.Client

object CustomMatchmaker extends App {

  import server.matchmaking.Matchmaker
  case class MyMatchMaker() extends Matchmaker[MyClientInfo] {
    override def createFairGroup(waitingClients: Map[Client, MyClientInfo]): Option[Map[Client, GroupId]] = {
      val admissibleGroups =
        for (clientX <- waitingClients.keys;
             clientY <- waitingClients.keys
             if clientX != clientY;
             x = waitingClients(clientX);
             y = waitingClients(clientY)
             if Math.abs(x.ranking - y.ranking) < 2
             if x.gender == y.gender)
        yield Map(clientX -> 1, clientY -> 2)
      admissibleGroups.headOption
    }
  }

  sealed trait Gender
  object Male extends Gender
  object Female extends Gender
  case class MyClientInfo(ranking: Int, gender: Gender)

  val clients =
    (0 until 10).map(i => Client.mock(s"$i") -> MyClientInfo(i, if(Math.random() < 0.5) Male else Female)).toMap
  val matchmaker = MyMatchMaker()
  val group = matchmaker createFairGroup clients
  println("Found group (if possible): " + group)
}
