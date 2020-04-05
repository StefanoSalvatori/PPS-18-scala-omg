package client

import akka.actor.ActorSystem
import akka.pattern.ask
import client.room.ClientRoom
import client.utils.MessageDictionary._
import common.SharedRoom.{RoomId, RoomType}
import common.{FilterOptions, RoomProperty, Routes}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}


sealed trait Client {

  /**
   * Creates a new public room the join
   *
   * @param roomType   type of room to create
   * @param roomProperties options
   * @return a future with the joined room
   */
  def createPublicRoom(roomType: RoomType, roomProperties: Set[RoomProperty]): Future[ClientRoom]

  /**
   * Join an existing room or create a new one, by provided roomType and options
   *
   * @param roomType   type of room to join
   * @param filterOption options to filter rooms for join
   * @param roomProperties property for room creation
   * @return a future with the joined room
   */
  def joinOrCreate(roomType: RoomType, filterOption: FilterOptions, roomProperties: Set[RoomProperty]): Future[ClientRoom]

  /**
   * Joins an existing room by provided roomType and options.
   * Fails if no room of such type exists
   *
   * @param roomType   type of room to join
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
  def joinById(roomId: RoomId): Future[ClientRoom]


  /**
   * @param roomType type of room to get
   * @param filterOptions options that will be used to filter the rooms
   * @return List all available rooms to connect of the given type
   */
  def getAvailableRoomsByType(roomType: String, filterOptions: FilterOptions): Future[Seq[ClientRoom]]


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

  private val serverUri = Routes.httpUri(serverAddress, serverPort)


  private implicit val actorSystem: ActorSystem = ActorSystem()
  private implicit val executor = actorSystem.dispatcher
  private val coreClient = actorSystem actorOf CoreClient(serverUri)


  override def joinedRooms(): Set[ClientRoom] =
    Await.result(coreClient ? GetJoinedRooms, timeout.duration).asInstanceOf[JoinedRooms].joinedRooms

  override def shutdown(): Unit = this.actorSystem.terminate()

  override def createPublicRoom(roomType: RoomType, roomProperties: Set[RoomProperty]): Future[ClientRoom] = {
    (coreClient ? CreatePublicRoom(roomType, roomProperties)) flatMap {
      case Success(room) => Future.successful(room.asInstanceOf[ClientRoom])
      case Failure(ex) => Future.failed(ex)
    }
  }

  override def joinOrCreate(roomType: RoomType, filterOption: FilterOptions, roomProperties: Set[RoomProperty]): Future[ClientRoom] = {
    this.join(roomType, filterOption) fallbackTo {
      this.createPublicRoom(roomType, roomProperties)
    }
  }

  override def join(roomType: RoomType, roomOption: FilterOptions): Future[ClientRoom] =
    (coreClient ? Join(roomType, roomOption)) flatMap {
      case Success(room) => Future.successful(room.asInstanceOf[ClientRoom])
      case Failure(ex) => Future.failed(ex)
    }

  override def joinById(roomId: RoomId): Future[ClientRoom] =
    (coreClient ? JoinById(roomId)) flatMap {
      case Success(room) => Future.successful(room.asInstanceOf[ClientRoom])
      case Failure(ex) => Future.failed(ex)
    }

  override def getAvailableRoomsByType(roomType: String, filterOption: FilterOptions): Future[Seq[ClientRoom]] =
    (coreClient ? GetAvailableRooms(roomType, filterOption)) flatMap {
      case Success(room) => Future.successful(room.asInstanceOf[Seq[ClientRoom]])
      case Failure(ex) => Future.failed(ex)
    }

}
