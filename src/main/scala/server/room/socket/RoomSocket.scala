package server.room.socket

import java.util.UUID

import akka.NotUsed
import akka.actor.{ActorRef, PoisonPill}
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import common.communication.CommunicationProtocol.ProtocolMessageType._
import common.communication.CommunicationProtocol.{RoomProtocolMessage, RoomProtocolMessageSerializer}
import common.room.Room.RoomPassword
import server.room.Client
import server.room.RoomActor.{Join, Leave, Msg}
import server.utils.Timer

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.{FiniteDuration, _}

object RoomSocket {
  val DefaultBufferSize: Int = Int.MaxValue
  val DefaultOverflowStrategy: OverflowStrategy = OverflowStrategy.dropHead

  //The maximum number of messages that the socket received but didn't finished to parse, When this limit is reached,
  // the socket will backpressure
  val MaxPendingMessages = 5000
}


/**
 * Define a socket flow that parse incoming messages as RoomProtocolMessages and convert them to messages that are
 * forwarded to a room actor.
 * It parses the messages with the parser specified in the constructor
 *
 * @param room the room actor that will receive the messages
 * @param parser the parsers to use for socket messages
 * @param connectionConfig socket connection configurations, see [[server.room.socket.ConnectionConfigurations]]
 * @param materializer an implicit materializer to run futures
 */
case class RoomSocket(private val room: ActorRef,
                      private val parser: RoomProtocolMessageSerializer,
                      private val connectionConfig: ConnectionConfigurations = ConnectionConfigurations.Default)
                     (implicit materializer: Materializer) {

  import RoomSocket._

  private implicit val executor: ExecutionContextExecutor = materializer.executionContext
  private val heartbeatTimer = Timer.withExecutor()
  private var heartbeatService: Option[ActorRef] = None
  private var socketPublisher: Option[ActorRef] = None

  def createFlow(overflowStrategy: OverflowStrategy = OverflowStrategy.dropHead,
                 bufferSize: Int = DefaultBufferSize): Flow[Message, Message, NotUsed] = {
    //Output (from room to client)
    val (socketActor, publisher) =
      Source.actorRef(PartialFunction.empty, PartialFunction.empty, bufferSize, overflowStrategy)
        .map(this.parser.prepareToSocket)
        .toMat(Sink.asPublisher(false))(Keep.both).run()
    this.socketPublisher = Some(socketActor)

    //Link this socket to the client
    val client = Client.asActor(UUID.randomUUID.toString, socketActor)
    //start heartbeat service
    if (connectionConfig.isKeepAliveActive) {
      this.startHeartbeat(client, connectionConfig.keepAlive.toSeconds seconds)
    }

    //Input (From client to room)
    val sink: Sink[Message, Any] = Flow[Message]
      .idleTimeout(connectionConfig.idleConnectionTimeout)
      .mapAsync[RoomProtocolMessage](parallelism = MaxPendingMessages)(x => {
        this.parser.parseFromSocket(x).recover {
          case _ => null // scalastyle:ignore null, null values are not passed downstream
        }
      }).collect({
      case RoomProtocolMessage(JoinRoom, "", payload) =>
        room ! Join(client, "", payload.asInstanceOf[RoomPassword])
      case RoomProtocolMessage(JoinRoom, sessionId, payload) =>
        room ! Join(Client.asActor(sessionId, socketActor), sessionId, payload.asInstanceOf[RoomPassword])
      case RoomProtocolMessage(LeaveRoom, _, _) =>
        room ! Leave(client)
      case RoomProtocolMessage(MessageRoom, _, payload) =>
        room ! Msg(client, payload)
      case RoomProtocolMessage(Pong, _, _) =>
        heartbeatService.foreach(_ ! Pong)
    }).to(Sink.onComplete(_ => {
      this.heartbeatTimer.stopTimer()
      heartbeatService.foreach(_ ! PoisonPill)
      room ! Leave(client)
    }))
    Flow.fromSinkAndSourceCoupled(sink, Source.fromPublisher(publisher))
  }

  private def startHeartbeat(client: Client, rate: FiniteDuration): Unit = {
    val heartbeatActor =
      Source.actorRef[ProtocolMessageType](PartialFunction.empty, PartialFunction.empty, Int.MaxValue, OverflowStrategy.fail)
        .toMat(Sink.fold(true) { (pongRcv, msg) => {
          msg match {
            case Ping if pongRcv => client.send(RoomProtocolMessage(Ping)); false
            case Ping => this.closeSocket(); false
            case Pong => true
          }
        }
        })(Keep.left).run()
    heartbeatTimer.scheduleAtFixedRate(() => heartbeatActor ! Ping, 0, connectionConfig.keepAlive.toMillis)
    this.heartbeatService = Some(heartbeatActor)
  }

  private def closeSocket(): Unit = {
    this.socketPublisher.foreach(_ ! PoisonPill)
  }
}
