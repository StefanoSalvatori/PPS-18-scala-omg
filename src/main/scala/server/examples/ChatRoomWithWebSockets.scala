package server.examples

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, WebSocketRequest}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Sink, Source}
import client.Client
import common.communication.CommunicationProtocol.{ProtocolMessageType, RoomProtocolMessage}
import common.http.Routes
import common.communication.TextProtocolSerializer
import server.GameServer
import server.examples.rooms.ChatRoom

import scala.concurrent.Await
import scala.io.StdIn
import scala.util.{Failure, Success}

object ChatRoomWithWebSockets extends App {
  implicit private val actorSystem: ActorSystem = ActorSystem()

  private val Host: String = "localhost"
  private val Port: Int = 8080
  private val EscapeExit = "quit"
  private val RoomPath = "chat"
  private val gameServer: GameServer = GameServer(Host, Port)
  private val client = Client(Host, Port)
  gameServer.defineRoom(RoomPath, ChatRoom)

  import scala.concurrent.duration._

  Await.ready(gameServer.start(), 10 seconds)
  val room = Await.result(client createPublicRoom RoomPath, 10 seconds)

  val webSocketRequest = WebSocketRequest(s"ws://$Host:$Port/${Routes.connectionRoute}/${room.roomId}")
  val webSocketFlow = Http().webSocketClientFlow(webSocketRequest)

  // Create a queue that streams messages through a websocket
  val queue = Source.queue[Message](Int.MaxValue, OverflowStrategy.dropTail)
    .viaMat(webSocketFlow)(Keep.left)
    .toMat(Sink.foreach(msg => {
      val protocolReceived = TextProtocolSerializer.parseFromSocket(msg)
      protocolReceived.onComplete {
        case Failure(_) => println("Received malformed message")
        case Success(value) => println(value)

      }(actorSystem.dispatcher)
    }))(Keep.left).run()

  //join chatroom
  this.joinRoom()
  //Start chat
  var msg = "Write 'quit' to exit"
  do {
    msg = StdIn.readLine()
    this.sendToRoom(msg)
  } while (msg != EscapeExit)

  Await.ready(gameServer.stop(), 10 seconds)
  Await.ready(gameServer.terminate(), 10 seconds)
  this.actorSystem.terminate()
  System.exit(0)

  private def sendToRoom(message: String): Unit = {
    this.queue.offer(TextProtocolSerializer.prepareToSocket(RoomProtocolMessage(ProtocolMessageType.MessageRoom, message)))
  }

  private def joinRoom() = {
    queue.offer(TextProtocolSerializer.prepareToSocket(RoomProtocolMessage(ProtocolMessageType.JoinRoom)))


  }


}



