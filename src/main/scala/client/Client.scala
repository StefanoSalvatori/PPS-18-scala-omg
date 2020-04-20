package client

import akka.actor.ActorSystem
import akka.pattern.ask
import client.room.{ClientRoom, JoinableRoom, JoinedRoom}
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
   * Create a new public room to join
   *
   * @param roomType       type of room to create
   * @param roomProperties room properties to set as the starting ones
   * @return a future with the joined room
   */
  def createPublicRoom(roomType: RoomType, roomProperties: Set[RoomProperty] = Set.empty): Future[JoinedRoom]

  /**
   * Create a private room.
   *
   * @param roomType       type of the room to create
   * @param roomProperties room properties to set as starting ones
   * @param password       password required for clients to join the room
   * @return a future with the joined room
   */
  def createPrivateRoom(roomType: RoomType, roomProperties: Set[RoomProperty] = Set.empty, password: RoomPassword): Future[JoinedRoom]

  /**
   * Join an existing room or create a new one, by provided roomType and options
   *
   * @param roomType       type of room to join
   * @param filterOption   options to filter rooms for join
   * @param roomProperties property for room creation
   * @return a future with the joined room
   */
  def joinOrCreate(roomType: RoomType, filterOption: FilterOptions, roomProperties: Set[RoomProperty] = Set.empty): Future[JoinedRoom]

  /**
   * Joins an existing room by provided roomType and options.
   * Fails if no room of such type exists
   *
   * @param roomType     type of room to join
   * @param filterOption filtering options
   * @return a future containing the joined room
   */
  def join(roomType: RoomType, filterOption: FilterOptions): Future[JoinedRoom]

  /**
   * Joins an existing room by its roomId.
   *
   * @param roomId the id of the room to join
   * @return a future with the joined room
   */
  def joinById(roomId: RoomId, password: RoomPassword = Room.defaultPublicPassword): Future[JoinedRoom]

  /**
   * @param roomType      type of room to get
   * @param filterOptions options that will be used to filter the rooms
   * @return List all available rooms to connect of the given type
   */
  def getAvailableRoomsByType(roomType: String, filterOptions: FilterOptions): Future[Seq[JoinableRoom]]

  /**
   * Reconnects the client into a room he was previously connected with.
   * The room should allow reconnection server-side
   *
   * @param roomId    room id
   * @param sessionId session id that was given by the room to this client
   * @return
   */
  def reconnect(roomId: String, sessionId: String): Future[JoinedRoom]

  /**
   * @return the set of currently joined rooms
   */
  def joinedRooms(): Set[JoinedRoom]

  def shutdown(): Unit
}

object Client {
  def apply(serverAddress: String, serverPort: Int): ClientImpl = new ClientImpl(serverAddress, serverPort)
}

private class ClientImpl(private val serverAddress: String, private val serverPort: Int) extends Client {

  private val requestTimeout = 5 // Seconds

  import akka.util.Timeout

  implicit val timeout: Timeout = requestTimeout seconds

  private val httpServerUri = Routes.httpUri(serverAddress, serverPort)

  private implicit val actorSystem: ActorSystem = ActorSystem()
  private implicit val executor: ExecutionContextExecutor = actorSystem.dispatcher
  private val coreClient = actorSystem actorOf CoreClient(httpServerUri)

  override def joinedRooms(): Set[JoinedRoom] = {
    Await.result(coreClient ? GetJoinedRooms, 5 seconds).asInstanceOf[JoinedRooms].joinedRooms
  }

  override def shutdown(): Unit = this.actorSystem.terminate()

  override def createPublicRoom(roomType: RoomType, roomProperties: Set[RoomProperty] = Set.empty): Future[JoinedRoom] =
    this createRoom CreatePublicRoom(roomType, roomProperties)

  override def createPrivateRoom(roomType: RoomType, roomProperties: Set[RoomProperty] = Set.empty, password: RoomPassword): Future[JoinedRoom] =
    this createRoom(CreatePrivateRoom(roomType, roomProperties, password), password)

  override def joinOrCreate(roomType: RoomType, filterOption: FilterOptions, roomProperties: Set[RoomProperty] = Set.empty): Future[JoinedRoom] = {
    this.join(roomType, filterOption) recoverWith {
      case _: Exception => this.createPublicRoom(roomType, roomProperties)
    }
  }

  override def join(roomType: RoomType, roomOption: FilterOptions): Future[JoinedRoom] =
    for {
      rooms <- getAvailableRoomsByType(roomType, roomOption)
      if rooms.nonEmpty
      toJoinRoom <- findJoinable(rooms)
    } yield {
      toJoinRoom
    }

  override def joinById(roomId: RoomId, password: RoomPassword = Room.defaultPublicPassword): Future[JoinedRoom] = {
    ifNotJoined(roomId, {
      val clientRoom = ClientRoom.createJoinable(coreClient, httpServerUri, roomId, Set())
      clientRoom.join(password)
    })
  }

  override def getAvailableRoomsByType(roomType: String, filterOption: FilterOptions): Future[Seq[JoinableRoom]] =
    (coreClient ? GetAvailableRooms(roomType, filterOption)) flatMap {
      case Success(room) => Future.successful(room.asInstanceOf[Seq[JoinableRoom]])
      case Failure(ex) => Future.failed(ex)
    }


  override def reconnect(roomId: String, sessionId: String): Future[JoinedRoom] = {
    ifNotJoined(roomId, {
      val clientRoom = ClientRoom.createJoinable(coreClient, httpServerUri, roomId, Set())
      clientRoom.joinWithSessionId(sessionId)
    })
  }

  /**
   * Perform the given action if the room with the specified id is not already joined.
   * If the room is joined return a failed future
   */
  private def ifNotJoined(idToCheck: RoomId, exec: => Future[JoinedRoom]): Future[JoinedRoom] = {
    if (this.joinedRooms().exists(_.roomId == idToCheck)) {
      Future.failed(new Exception("Room already joined"))
    } else {
      exec
    }
  }

  private def createRoom(message: CreateRoomMessage, password: RoomPassword = Room.defaultPublicPassword): Future[JoinedRoom] = {
    for {
      room <- coreClient ? message
      clientRoomTry = room.asInstanceOf[Try[JoinableRoom]]
      if clientRoomTry.isSuccess
      clientRoom = clientRoomTry.get
      joinedRoom <- clientRoom.join(password)
    } yield {
      joinedRoom
    }
  }

  /**
   * @param rooms to check
   * @return the first joinable room in the sequence
   */
  @scala.annotation.tailrec
  private def findJoinable(rooms: Seq[JoinableRoom]): Future[JoinedRoom] =
    rooms match {
      case head +: _ =>
        try {
          Future.successful(Await.result(head.join(), 5 seconds))
        } catch {
          case _: Exception => findJoinable(rooms.tail)
        }
      case Nil => Future.failed(new Exception("no room to join"))

    }
}
