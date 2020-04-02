package client.room

import akka.{Done, NotUsed}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{CompletionStrategy, OverflowStrategy}
import common.CommonRoom.{Room, RoomId}
import common.actors.ApplicationActorSystem
import akka.util.Timeout
import org.reactivestreams.Publisher

import scala.concurrent.Future
import scala.concurrent.duration._


object ClientRoom extends ApplicationActorSystem {
  implicit val timeout: Timeout = 5 seconds

  case class ConnectedToServer(actorRef: ActorRef)

  case class MessageFromServer(msg: Any)

  case class JoinRoom(roomId: RoomId)


  trait ClientRoom extends Room {

    def join(): Future[ClientRoom]

    def leave(): Future[Done]

    def send(msg: Any): Future[Done]

    def onMessage(callback: String => Unit): Any
  }

  object ClientRoom {
    def apply(roomId: RoomId): ClientRoom = ClientRoomImpl(roomId)
  }


  case class ClientRoomImpl(roomId: RoomId) extends ClientRoom with ApplicationActorSystem {
    private val buffSize = 100
    val clientRoomActor = actorSystem.actorOf(Props(classOf[ClientRoomActor], roomId))

    private def initServerSource: (ActorRef, Publisher[TextMessage.Strict]) =
      Source.actorRef({ case Done => CompletionStrategy.draining },
        PartialFunction.empty, buffSize, OverflowStrategy.dropHead)
        .map(msg => TextMessage.Strict(msg))
        .toMat(Sink.asPublisher(false))(Keep.both).run()

    private def sink(): Sink[Message, NotUsed] = Flow[Message].map {
      case TextMessage.Strict(msg) =>
        // Incoming message from ws
        clientRoomActor ! MessageFromServer(msg)
    }.to(Sink.onComplete(_ => println("sink complete")))

    override def join(): Future[ClientRoom] = {
      /*val (actorRef: ActorRef, publisher: mutable.Publisher[TextMessage.Strict]) = initServerSource
      clientRoomActor ! ConnectedToServer(actorRef)
      val wsReq = HttpRequests.connectToRoom(serverUri)(roomId)
      val flow = Flow.fromSinkAndSource(sink(), Source.fromPublisher(publisher))
      val (res, _) = Http() singleWebSocketRequest(wsReq, flow)
      for {
        _ <- res
        _ <- clientRoomActor ? JoinRoom(this.roomId)
      } yield this*/
      Future.successful(this)
    }


    override def leave(): Future[Done] = ???

    override def send(msg: Any): Future[Done] = ???

    override def onMessage(callback: String => Unit): Any = ???


  }

  private class ClientRoomActor(roomId: RoomId) extends Actor with ActorLogging {
    private var serverRef: ActorRef = _

    override def receive: Receive = {
      case ConnectedToServer(ref) => serverRef = ref
      case MessageFromServer(msg) => msg match {
        case "join" => println("join OK!")
      }
      case JoinRoom =>
        serverRef ! "join"
    }
  }


}


