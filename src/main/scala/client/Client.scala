package client

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.pattern.ask
import client.room.ClientRoom
import client.utils.MessageDictionary._
import common.http.Routes
import common.room.Room.{RoomId, RoomPassword, RoomType}
import common.room.{FilterOptions, Room, RoomProperty}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future, Promise}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

sealed trait Client {

  /**
   * Creates a new public room the join
   *
   * @param roomType       type of room to create
   * @param roomProperties options
   * @return a future with the joined room
   */
  def createPublicRoom(roomType: RoomType, roomProperties: Set[RoomProperty] = Set.empty): Future[ClientRoom]

  /**
   * Create a private room.
   *
   * @param roomType       type of the room to create
   * @param roomProperties room options to set as starting ones
   * @param password       password required for clients to join the room
   */
  def createPrivateRoom(roomType: RoomType, roomProperties: Set[RoomProperty] = Set.empty, password: RoomPassword): Future[ClientRoom]

  /**
   * Join an existing room or create a new one, by provided roomType and options
   *
   * @param roomType       type of room to join
   * @param filterOption   options to filter rooms for join
   * @param roomProperties property for room creation
   * @return a future with the joined room
   */
  def joinOrCreate(roomType: RoomType, filterOption: FilterOptions, roomProperties: Set[RoomProperty] = Set.empty): Future[ClientRoom]

  /**
   * Joins an existing room by provided roomType and options.
   * Fails if no room of such type exists
   *
   * @param roomType     type of room to join
   * @param filterOption filtering options
   * @return a future containing the joined room
   */
  def join(roomType: RoomType, filterOption: FilterOptions): Future[ClientRoom]

  /**
   * Joins an existing room by its roomId.
   *
   * @param roomId the id of the room to join
   * @return a future with the joined room
   */
  def joinById(roomId: RoomId, password: RoomPassword = Room.defaultPublicPassword): Future[ClientRoom]

  /**
   * @param roomType      type of room to get
   * @param filterOptions options that will be used to filter the rooms
   * @return List all available rooms to connect of the given type
   */
  def getAvailableRoomsByType(roomType: String, filterOptions: FilterOptions): Future[Seq[ClientRoom]]


  /**
   * Reconnects the client into a room he was previously connected with.
   * The room should allow reconnection server-side
   *
   * @param roomId    room id
   * @param sessionId session id that was given by the room to this client
   * @return
   */
  def reconnect(roomId: String, sessionId: String): Future[ClientRoom]

  /**
   * @return the set of currently joined rooms
   */
  def joinedRooms(): Set[ClientRoom]

  def shutdown(): Unit
}

object Client {
  def apply(serverAddress: String, serverPort: Int): ClientImpl = new ClientImpl(serverAddress, serverPort)
}

class ClientImpl(private val serverAddress: String, private val serverPort: Int) extends Client {

  private val requestTimeout = 5 // Seconds

  import akka.util.Timeout

  implicit val timeout: Timeout = requestTimeout seconds

  private val httpServerUri = Routes.httpUri(serverAddress, serverPort)

  private implicit val actorSystem: ActorSystem = ActorSystem()
  private implicit val executor: ExecutionContextExecutor = actorSystem.dispatcher
  private val coreClient = actorSystem actorOf CoreClient(httpServerUri)

  override def joinedRooms(): Set[ClientRoom] = {
    Await.result(coreClient ? GetJoinedRooms, 5 seconds).asInstanceOf[JoinedRooms].joinedRooms
  }

  override def shutdown(): Unit = this.actorSystem.terminate()

  override def createPublicRoom(roomType: RoomType, roomProperties: Set[RoomProperty] = Set.empty): Future[ClientRoom] =
    this createRoom CreatePublicRoom(roomType, roomProperties)

  override def createPrivateRoom(roomType: RoomType, roomProperties: Set[RoomProperty] = Set.empty, password: RoomPassword): Future[ClientRoom] =
    this createRoom(CreatePrivateRoom(roomType, roomProperties, password), password)

  override def joinOrCreate(roomType: RoomType, filterOption: FilterOptions, roomProperties: Set[RoomProperty] = Set.empty): Future[ClientRoom] = {
    this.join(roomType, filterOption) recoverWith {
      case _: Exception => this.createPublicRoom(roomType, roomProperties)
    }
  }

  override def join(roomType: RoomType, roomOption: FilterOptions): Future[ClientRoom] =
    for {
      rooms <- getAvailableRoomsByType(roomType, roomOption)
      if rooms.nonEmpty
      toJoinRoom <- findJoinable(rooms)
    } yield {
      toJoinRoom
    }

  private def firstSucceededOf[T](futures: TraversableOnce[Future[T]]): Future[T] = {
    val p = Promise[T]()
    val size = futures.size
    val failureCount = new AtomicInteger(0)

    futures foreach {
      _.onComplete {
        case Success(v) => p.trySuccess(v)
        case Failure(e) =>
          val count = failureCount.incrementAndGet
          if (count == size) p.tryFailure(e)
      }
    }
    p.future
  }

  private def findJoinable(rooms: Seq[ClientRoom]): Future[ClientRoom] = {
    firstSucceededOf {
      rooms.map { room =>
        for {_ <- room.join()} yield {
          room
        }
      }
    }
  }

  override def joinById(roomId: RoomId, password: RoomPassword = Room.defaultPublicPassword): Future[ClientRoom] = {
    ifNotJoined(roomId, {
      val clientRoom = ClientRoom(coreClient, httpServerUri, roomId, Map())
      clientRoom.join(password).map(_ => clientRoom)
    })
  }

  override def getAvailableRoomsByType(roomType: String, filterOption: FilterOptions): Future[Seq[ClientRoom]] =
    (coreClient ? GetAvailableRooms(roomType, filterOption)) flatMap {
      case Success(room) => Future.successful(room.asInstanceOf[Seq[ClientRoom]])
      case Failure(ex) => Future.failed(ex)
    }


  override def reconnect(roomId: String, sessionId: String): Future[ClientRoom] = {
    ifNotJoined(roomId, {
      val clientRoom = ClientRoom(coreClient, httpServerUri, roomId, Map(), sessionId)
      clientRoom.join().map(_ => clientRoom)
    })
  }

  /**
   * Perform the given action if the room with the specified id is not already joined.
   * If the room is joined return a failed future
   */
  private def ifNotJoined(idToCheck: RoomId, exec: => Future[ClientRoom]): Future[ClientRoom] = {
    if (this.joinedRooms().exists(_.roomId == idToCheck)) {
      Future.failed(new Exception("Room already joined"))
    } else {
      exec
    }
  }

  private def createRoom(message: CreateRoomMessage, password: RoomPassword = Room.defaultPublicPassword): Future[ClientRoom] = {
    for {
      room <- coreClient ? message
      clientRoomTry = room.asInstanceOf[Try[ClientRoom]]
      if clientRoomTry.isSuccess
      clientRoom = clientRoomTry.get
      _ <- clientRoom.join(password)
    } yield {
      clientRoom
    }
  }
}
