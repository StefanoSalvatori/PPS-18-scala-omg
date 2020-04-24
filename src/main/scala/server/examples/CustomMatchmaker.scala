package server.examples
import server.matchmaking.Group.GroupId
import server.room.Client

object CustomMatchmaker extends App {

  import server.matchmaking.Matchmaker
  case class MyMatchmaker() extends Matchmaker[MyClientInfo] {
    override def createFairGroup(waitingClients: Map[Client, MyClientInfo]): Option[Map[Client, GroupId]] = {
      val admissibleGroups =
        // Create two groups, each one containing two clients
        for ( // Generate all possible combinations of 4 clients (x,y,z,w)
             clientX <- waitingClients.keys; clientY <- waitingClients.keys;
             clientZ <- waitingClients.keys; clientW <- waitingClients.keys
             // Drop combinations where 1 or more clients are the same
             if clientX != clientY && clientX != clientZ && clientX != clientW
             if clientY != clientZ && clientY != clientW
             if clientZ != clientW;
             // Retrieve clients information using their Ids
             x = waitingClients(clientX); y = waitingClients(clientY);
             z = waitingClients(clientZ); w = waitingClients(clientW)
             // Check ranking constraint: the difference of total team ranking is lower than 2
             if Math.abs((x.ranking + y.ranking) - (z.ranking + w.ranking)) < 2
             // Check gender constraint: each team should have 1 male and 1 female
             if x.gender == Male && x.gender == z.gender
             if y.gender == Female && y.gender == w.gender
             )
        yield Map(clientX -> 1, clientY -> 1, clientZ -> 2, clientW -> 2)
      admissibleGroups.headOption
    }
  }

  sealed trait Gender
  object Male extends Gender
  object Female extends Gender
  case class MyClientInfo(ranking: Int, gender: Gender)

  val clients =
    (0 until 10).map(i => Client.mock(s"$i") -> MyClientInfo(i, if(Math.random() < 0.5) Male else Female)).toMap
  val matchmaker = MyMatchmaker()
  val group = matchmaker createFairGroup clients
  println("Found group (if possible): " + group)
}
