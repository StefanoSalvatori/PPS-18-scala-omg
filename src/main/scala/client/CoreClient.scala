package client

import akka.actor.{Actor, ActorSystem}

sealed trait CoreClient extends Actor

object CoreClient {
  import akka.actor.Props
  def apply(serverUri: String): Props = Props(classOf[CoreClientImpl], serverUri)
}

class CoreClientImpl(private val serverUri: String) extends CoreClient {

  implicit val system: ActorSystem = context.system
  import scala.concurrent.ExecutionContext
  implicit val executionContext: ExecutionContext = system.dispatcher

  private val httpClient = context.system actorOf HttpClient(serverUri)

  import MessageDictionary._
  override def receive: Receive = {

    case CreatePublicRoom =>
      httpClient ! CreatePublicRoom

    case _ =>
      print("Ignoring unknown message: " + _)
      sender ! UnknownMessageReply
  }
}