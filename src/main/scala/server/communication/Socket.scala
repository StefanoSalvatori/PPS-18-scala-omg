package server.communication

import java.util.UUID

import akka.NotUsed
import akka.actor.{ActorRef, PoisonPill}
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{ActorAttributes, Materializer, OverflowStrategy, Supervision}
import common.communication.SocketSerializer
import server.room.Client
import server.utils.Timer

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.{FiniteDuration, _}

object Socket {
  val DefaultOverflowStrategy: OverflowStrategy = OverflowStrategy.dropHead

  //The maximum number of messages that the socket received but didn't finished to parse. When this limit is reached,
  // the input stream of the socket will backpressure
  val DefaultMaxInputPendingMessages = 10000

  // The maximum number of messages that the socket has in the outbox queue.When this limit is reached,
  // the output stream of the socket will backpressure
  val DefaultMaxOutputPendingMessages: Int = 10000

}

trait Socket[T] {

  import Socket._
  val connectionConfig: ConnectionConfigurations = ConnectionConfigurations.Default
  val overflowStrategy: OverflowStrategy = DefaultOverflowStrategy
  val outBufferSize: Int = DefaultMaxOutputPendingMessages
  val inBufferSize: Int = DefaultMaxInputPendingMessages

  protected val parser: SocketSerializer[T]
  protected val pingMessage: T
  protected val pongMessage: T

  //The client connected to this socket.
  // It is created when createFlow is called and it will be linked to the clientActor
  protected var client: Client = _
  protected var clientActor: ActorRef = _

  private val heartbeatTimer = Timer.withExecutor()
  private var heartbeatServiceActor: Option[ActorRef] = None

  def open()(implicit materializer: Materializer): Flow[Message, Message, NotUsed] = {
    implicit val executor: ExecutionContextExecutor = materializer.executionContext
    val (socketOutputActor, publisher) = this.outputStream.run()

    //Link this socket to the client
    this.clientActor = socketOutputActor
    this.client = Client.asActor(UUID.randomUUID.toString, this.clientActor)

    if (connectionConfig.isKeepAliveActive) {
      this.startHeartbeat(client, connectionConfig.keepAlive.toSeconds seconds)
    }

    val sink: Sink[Message, Any] =
      this.inputStream.to(Sink.onComplete(_ => {
        this.heartbeatTimer.stopTimer()
        heartbeatServiceActor.foreach(_ ! PoisonPill)
        this.onSocketClosed()
      }))
    Flow.fromSinkAndSourceCoupled(sink, Source.fromPublisher(publisher))
  }

  def close(): Unit = {
    if (this.clientActor != null) {
      this.clientActor ! PoisonPill
    }
  }

  protected val onMessageFromSocket: PartialFunction[T, Unit]

  protected def onSocketClosed(): Unit

  //Input (From client to socket)
  private def outputStream = {
    Source.actorRef(PartialFunction.empty, PartialFunction.empty, outBufferSize, overflowStrategy)
      .map(this.parser.prepareToSocket)
      .toMat(Sink.asPublisher(false))(Keep.both)
  }

  //Output (from socket to client)
  private def inputStream = {
    var incomingFlow = Flow[Message].withAttributes(ActorAttributes.supervisionStrategy(Supervision.resumingDecider))
    if (connectionConfig.isIdleTimeoutActive) {
      incomingFlow = incomingFlow.idleTimeout(connectionConfig.idleConnectionTimeout.toSeconds seconds)
    }
    incomingFlow
      .mapAsync[T](parallelism = DefaultMaxInputPendingMessages)(this.parser.parseFromSocket)
      .collect(onPongMessage.orElse(onMessageFromSocket))
  }

  //Start heartbeat to a specific client
  private def startHeartbeat(client: Client, rate: FiniteDuration)
                            (implicit materializer: Materializer): Unit = {
    val heartbeatActor =
      Source.actorRef[T](PartialFunction.empty, PartialFunction.empty, Int.MaxValue, OverflowStrategy.fail)
        .toMat(Sink.fold(true)((pongRcv, msg) => {
          msg match {
            case this.pingMessage if pongRcv => client.send(pingMessage); false
            case this.pingMessage => this.close(); false
            case this.pongMessage => true
          }
        }))(Keep.left).run()
    this.heartbeatServiceActor = Some(heartbeatActor)
    heartbeatTimer.scheduleAtFixedRate(() => heartbeatActor ! this.pingMessage, 0, connectionConfig.keepAlive.toMillis)
  }


  private def onPongMessage: PartialFunction[T, Any] = {
    case this.pongMessage => this.heartbeatServiceActor.foreach(_ ! this.pongMessage)
  }
}
