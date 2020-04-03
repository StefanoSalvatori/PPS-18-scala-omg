package client

import akka.pattern.ask
import common.actors.ApplicationActorSystem
import client.MessageDictionary._
import client.room.ClientRoom
import common.CommonRoom.{RoomId, RoomType}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps


sealed trait Client {

  /**
   * Creates a new public room the join
   *
   * @param roomType   type of room to create
   * @param roomOption options
   * @return a future with the joined room
   */
  def createPublicRoom(roomType: RoomType, roomOption: Any): Future[ClientRoom]

  /**
   * Join an existing room or create a new one, by provided roomType and options
   *
   * @param roomType   type of room to join
   * @param roomOption filtering options
   * @return a future with the joined room
   */
  def joinOrCreate(roomType: RoomType, roomOption: Any): Future[ClientRoom]

  /**
   * Joins an existing room by provided roomType and options.
   * Fails if no room of such type exists
   *
   * @param roomType   type of room to join
   * @param roomOption filtering options
   * @return a future containing the joined room
   */
  def join(roomType: RoomType, roomOption: Any): Future[ClientRoom]

  /**
   * Joins an existing room by its roomId.
   *
   * @param roomId the id of the room to join
   * @return a future with the joined room
   */
  def joinById(roomId: RoomId): Future[ClientRoom]


  /**
   * @param roomType type of room to get
   * @return List all available rooms to connect of the given type
   */
  def getAvailableRoomsByType(roomType: String): Future[Seq[ClientRoom]]

  def joinedRooms(): Set[ClientRoom]

  def shutdown(): Unit
}

object Client {
  def apply(serverAddress: String, serverPort: Int): ClientImpl = new ClientImpl(serverAddress, serverPort)
}

class ClientImpl(private val serverAddress: String, private val serverPort: Int) extends Client {

  private val requestTimeout = 5 // Seconds

  import common.actors.ApplicationActorSystem._
  import akka.util.Timeout

  implicit val timeout: Timeout = requestTimeout seconds

  private val serverUri = "http://" + serverAddress + ":" + serverPort

  private val coreClient = actorSystem actorOf CoreClient(serverUri)

  /*override def createPublicRoom(roomType: RoomType, roomOption: Any): Unit =
    coreClient ! CreatePublicRoom*/

  override def joinedRooms(): Set[ClientRoom] =
    Await.result(coreClient ? GetJoinedRooms, timeout.duration).asInstanceOf[JoinedRooms].rooms

  override def shutdown(): Unit = terminateActorSystem()

  override def createPublicRoom(roomType: RoomType, roomOption: Any): Future[ClientRoom] =
    (coreClient ? CreatePublicRoom(roomType, roomOption)).mapTo[ClientRoom]

  override def joinOrCreate(roomType: RoomType, roomOption: Any): Future[ClientRoom] =
    (coreClient ? JoinOrCreate(roomType, roomOption)).mapTo[ClientRoom]

  override def join(roomType: RoomType, roomOption: Any): Future[ClientRoom] =
    (coreClient ? Join(roomType, roomOption)).mapTo[ClientRoom]

  override def joinById(roomId: RoomId): Future[ClientRoom] =
    (coreClient ? JoinById(roomId)).mapTo[ClientRoom]

  override def getAvailableRoomsByType(roomType: String): Future[Seq[ClientRoom]] =
    (coreClient ? GetAvailableRooms(roomType)).mapTo[Seq[ClientRoom]]
}
