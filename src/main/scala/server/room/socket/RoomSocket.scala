package server.room.socket

import java.util.UUID

import akka.NotUsed
import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import common.communication.CommunicationProtocol.{ProtocolMessageType, RoomProtocolMessage, RoomProtocolMessageSerializer}
import common.room.Room.RoomPassword
import server.communication.SocketFlow
import server.room.Client
import server.room.RoomActor.{Join, Leave, Msg}
import scala.concurrent.Future
import scala.util.{Failure, Success}


/**
 * Define a socket flow that parse incoming messages as RoomProtocolMessages and convert them to messages that are
 * forwarded to a room actor.
 * It parses the messages with the parser specified in the constructor
 *
 * @param roomActor the actor that will receive the messages
 */
case class RoomSocket(private val roomActor: ActorRef,
                      private val parser: RoomProtocolMessageSerializer)
                     (implicit materializer: Materializer) extends SocketFlow {

  private implicit val executor = materializer.executionContext


  def createFlow(overflowStrategy: OverflowStrategy = OverflowStrategy.dropHead,
                 bufferSize: Int = DefaultBufferSize): Flow[Message, Message, NotUsed]
  = {
    //Output (from room to client)
    val (socketActor, publisher) = Source.actorRef(PartialFunction.empty, PartialFunction.empty, bufferSize, overflowStrategy)
      .map(this.parser.prepareToSocket)
      .toMat(Sink.asPublisher(false))(Keep.both).run()

    //Link this socket to the client
    var client = Client.asActor(UUID.randomUUID.toString, socketActor)


    //Input (From client to room)
    val sink: Sink[Message, Any] = Flow[Message]
      .mapAsync[RoomProtocolMessage](parallelism = Int.MaxValue)(x =>
        this.parser.parseFromSocket(x).recover { case _ => null } // scalastyle:ignore null
        // null values are not passed downstream
      )
      .collect({
        case RoomProtocolMessage(ProtocolMessageType.JoinRoom, sessionId, payload) =>
          //if sessionId is given create the client with that id
          if (!sessionId.isEmpty) {
            client = Client.asActor(sessionId, socketActor)
          }
          roomActor ! Join(client, sessionId, payload.asInstanceOf[RoomPassword])
        case RoomProtocolMessage(ProtocolMessageType.LeaveRoom, _, _) =>
          roomActor ! Leave(client)
        case RoomProtocolMessage(ProtocolMessageType.MessageRoom, _, payload) =>
          roomActor ! Msg(client, payload)
        case _ =>
      })
      .to(Sink.onComplete(_ => {
        roomActor ! Leave(client)
      }))


    Flow.fromSinkAndSourceCoupled(sink, Source.fromPublisher(publisher))

  }
}
