package examples.rock_paper_scissor.server

import java.io.FileInputStream

import alice.tuprolog.{Prolog, Theory}
import server.room.{Client, ServerRoom}

class ClassicMatchRoom() extends ServerRoom {

  private val MaxPlayers = 2
  private var gameState: Seq[(Client, String)] = Seq.empty
  private val gameLogic = RockPaperScissor()

  def availableMoves: Set[String] = Set("rock", "paper", "scissor")


  override def onCreate(): Unit = {
    println("battle room  " + roomId + " created ")
  }

  override def onClose(): Unit = println("match room  " + roomId + " closed ")

  override def onJoin(client: Client): Unit = {
    //start the game as soon as we reach max players
    if (reachedMaxPlayers) {
      //TODO: lock the room
      //notify connected clients that the game started
      this.broadcast("start")
    }

    println(s"client ${client.id} joined")

  }


  override def onLeave(client: Client): Unit = println("client+ " + client.id + " left ")

  override def onMessageReceived(client: Client, message: Any): Unit = {
    if (checkMove(message.toString)) {
      gameState = gameState.+:((client, message.toString))

      //check result as soon as we receive the last player move
      if (receivedAllMoves) {
        val players = gameState.map(_._1)
        val moves = gameState.map(_._2)
        val PlayerOneMove: String = moves.head
        val PlayerTwoMove: String = moves(1)
        val result = gameLogic.result(PlayerOneMove, PlayerTwoMove)
        result match {
          case PlayerOneMove =>
            this.tell(players.head, "win")
            this.tell(players(1), "lose")
          case PlayerTwoMove =>
            this.tell(players.head, "lose")
            this.tell(players(1), "win")
          case _ =>
            this.broadcast("draw")
        }

        this.close()
      }

    }


    println(s"${client.id }: $message")
  }

  private def checkMove(move: String) = this.availableMoves.contains(move)

  private def reachedMaxPlayers = this.connectedClients.size == MaxPlayers

  private def receivedAllMoves = gameState.size == MaxPlayers

}


class AdvancedMatchRoom() extends ClassicMatchRoom {
  override def availableMoves: Set[String] = Set("rock", "paper", "scissor", "lizard", "spock")
}


/**
 * Rock paper scissor logic implemented in prolog
 */
case class RockPaperScissor() {
  private val TheoryPath = "src/main/scala/examples/rock_paper_scissor/server/resources/rock_scissor_paper.prolog"
  private val TermResult = "Z"
  private val engine = new Prolog()
  private val theory = new Theory(new FileInputStream(TheoryPath))
  engine.setTheory(theory)


  private def goal(p1: String, p2: String) = s"win($p1,$p2,$TermResult)."

  def result(p1: String, p2: String): String = {
    engine.solve(goal(p1, p2)).getTerm(TermResult).toString
  }
}
