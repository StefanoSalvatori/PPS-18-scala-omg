package server.room

import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{CompletionStrategy, Materializer, OverflowStrategy}
import server.room.RoomActor.{Join, Leave, Msg}


object WebSocketHandler {
  private val BUFFER_SIZE: Int = 16

  private val COMMAND_SEPARATOR = "::"

  sealed trait ClientMessage
  case object EndStream extends ClientMessage
  case class Failure(exception: Throwable) extends ClientMessage
  case class StringMessage(msg: String) extends ClientMessage
  // case class Msg[T](msg: T) extends ClientMessage


  /**
   * The handler of the client/server web socket
   *
   * @param client    the id of yhe client linked to this websocket
   * @param roomActor the room the this socket handler will talk to
   * @return a flow that repsrents the handler of the socket
   */
  def websocketFlow(client: Client, roomActor: ActorRef)(implicit materializer: Materializer): Flow[Message, Message, Any] = {
    val (clientActor, publisher) =
      Source.actorRef[ClientMessage](completionMatcher, failMatcher, BUFFER_SIZE, OverflowStrategy.dropTail)
        .map({
          case StringMessage(msg) => TextMessage.Strict(msg)
          case _ => TextMessage.Strict("")
        })
        .toMat(Sink.asPublisher(false))(Keep.both).run()

    // roomActor ! Join(Client(clientId))

    val sink: Sink[Message, Any] = Flow[Message]
      .map {
        case TextMessage.Strict(msg) =>
          val (command, payload) = this.parseMessage(msg)
          command match {
            case 0 => roomActor ! Join(client)
            case 1 => roomActor ! Msg(client, payload)
            case 2 => roomActor ! Leave(client)
          }
        case _ =>
      }
      .to(Sink.onComplete(_ => {})) // Announce the user has left))

    Flow.fromSinkAndSource(sink, Source.fromPublisher(publisher))

  }

  /**
   * @return the partial function to tell how whe should complete the stream
   */
  private def completionMatcher: PartialFunction[Any, CompletionStrategy] = {
    case EndStream => CompletionStrategy.draining
  }

  /**
   * @return the partial function to tell how whe should fail the stream
   */
  private def failMatcher: PartialFunction[Any, Throwable] = {
    case Failure(exception) => exception
  }

  private def parseMessage(msg: String): (Int, String) = {
    (msg.split(COMMAND_SEPARATOR)(0).toInt, msg.split(COMMAND_SEPARATOR)(1))
  }

}

