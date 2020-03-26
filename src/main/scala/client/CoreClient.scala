package client

sealed trait CoreClient extends BasicActor

object CoreClient {
  import akka.actor.Props
  def apply(serverUri: String): Props = Props(classOf[CoreClientImpl], serverUri)
}

class CoreClientImpl(private val serverUri: String) extends CoreClient {

  private val httpClient = context.system actorOf HttpClient(serverUri, self)

  private var joinedRooms: Set[String] = Set()

  println(joinedRooms.getClass)

  import MessageDictionary._
  val onReceive: PartialFunction[Any, Unit] = {
    case CreatePublicRoom =>
      httpClient ! CreatePublicRoom
    case JoinedRoom(roomId) =>
      joinedRooms += roomId
  }

  override def receive: Receive = onReceive orElse fallbackReceive
}