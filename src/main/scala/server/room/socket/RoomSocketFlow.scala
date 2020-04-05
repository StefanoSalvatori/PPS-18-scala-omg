package server.room.socket

import java.util.UUID

import akka.NotUsed
import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import common.communication.CommunicationProtocol.{JoinRoom, LeaveRoom, MessageRoom, RoomProtocolMessage}
import common.communication.SocketSerializer
import server.communication.SocketFlow
import server.room.Client
import server.room.RoomActor.{Join, Leave, Msg}

import scala.util.Success


/**
 * Define a socket flow that parse incoming messages as RoomProtocolMessages and convert them to messages that are
 * forwarded to a room actor.
 * It parses the messages with the parser specified in the constructor
 *
 * @param roomActor the actor that will receive the messages
 */
case class RoomSocketFlow(private val roomActor: ActorRef,
                          private val parser: SocketSerializer[RoomProtocolMessage])
                         (implicit materializer: Materializer) extends SocketFlow {


  def createFlow(overflowStrategy: OverflowStrategy = OverflowStrategy.dropHead,
                 bufferSize: Int = DEFAULT_BUFFER_SIZE): Flow[Message, Message, NotUsed]
  = {
    //Output (from room to client)
    val (socketActor, publisher) = Source.actorRef(
      PartialFunction.empty,
      PartialFunction.empty,
      bufferSize, overflowStrategy)
      .map(this.parser.writeToSocket)
      .toMat(Sink.asPublisher(false))(Keep.both).run()

    //Link this socket to the client
    val client = Client.asActor(UUID.randomUUID.toString, socketActor)

    //Input (From client to room)
    val sink: Sink[Message, Any] = Flow[Message]
      .map {
        this.parser.parseFromSocket(_) match {
          case Success(RoomProtocolMessage(JoinRoom, _)) => roomActor ! Join(client)
          case Success(RoomProtocolMessage(LeaveRoom, _)) => roomActor ! Leave(client)
          case Success(RoomProtocolMessage(MessageRoom, payload)) => roomActor ! Msg(client, payload)
        }
      }
      .to(Sink.onComplete(_ => {}))

    Flow.fromSinkAndSourceCoupled(sink, Source.fromPublisher(publisher))

  }
}
