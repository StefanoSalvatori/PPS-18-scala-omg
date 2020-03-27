package server.examples

import server.GameServer
import server.room.RoomStrategy

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}
import scala.io.StdIn

object DefineRoomType extends App {
  implicit val executor: ExecutionContextExecutor = ExecutionContext.global

  val HOST: String = "localhost"
  val PORT: Int = 8080
  val ROOM_TYPE_NAME: String = "test_room"

  val gameServer: GameServer = GameServer(HOST, PORT)
  gameServer onStart {
    println("GAMESERVER STARTED")
  }
  gameServer onShutdown {
    println("GAMESERVER IS DOWN :-(")
  }

  gameServer.defineRoom(ROOM_TYPE_NAME, new RoomStrategy {
    override def onJoin(): Unit = {}
    override def onMessageReceived(): Unit = {}
    override def onLeave(): Unit = {}
    override def onCreate(): Unit = {}
  })

  Await.ready(gameServer.start(), 10 seconds)
  println(s"try http://$HOST:$PORT/rooms/$ROOM_TYPE_NAME from your browser")
  println("press any key to shutdown...")
  StdIn.readLine()
  gameServer.shutdown()
}