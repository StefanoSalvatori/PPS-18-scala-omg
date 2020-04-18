package server.room.socket

import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import akka.NotUsed
import akka.actor.{ActorRef, Cancellable, PoisonPill}
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
 * @param room the actor that will receive the messages
 */
case class RoomSocket(private val room: ActorRef,
                      private val parser: RoomProtocolMessageSerializer,
                      private val connectionConfig: ConnectionConfigurations = ConnectionConfigurations.Default)
                     (implicit materializer: Materializer) {

  import RoomSocket._

  private implicit val executor: ExecutionContextExecutor = materializer.executionContext
  private val heartbeatTimer = Timer.withExecutor()
  private var heartbeatService: Option[Cancellable] = None
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
      this.heartbeatService = Some(startHeartbeat(client, connectionConfig.keepAlive.toSeconds seconds))
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
        heartbeatTimer.stopTimer()
    }).to(Sink.onComplete(_ => {
      heartbeatService.foreach(_.cancel())
      room ! Leave(client)
    }))
    Flow.fromSinkAndSourceCoupled(sink, Source.fromPublisher(publisher))
  }

  private def startHeartbeat(client: Client, rate: FiniteDuration): Cancellable = {
    Source.tick(0 seconds, rate, ())
      .map(_ => {
        client.send(RoomProtocolMessage(Ping))
        heartbeatTimer.scheduleOnce(() => this.closeSocket(), connectionConfig.keepAlive.toMillis)
      }).toMat(Sink.ignore)(Keep.left).run()
  }

  private def closeSocket(): Unit = {
    this.socketPublisher.foreach(_ ! PoisonPill)
  }
}
