package server.room

import java.lang.reflect.Field
import akka.actor.ActorRef
import common.communication.CommunicationProtocol.ProtocolMessageType._
import common.communication.CommunicationProtocol.{ProtocolMessage, ProtocolMessageType, SocketSerializable}
import common.room.Room.{RoomId, RoomPassword}
import common.room._
import server.communication.ConnectionConfigurations
import server.room.RoomActor.{Close, StartAutoCloseTimeout}
import server.utils.Timer

trait ServerRoom extends BasicRoom
  with PrivateRoomSupport
  with RoomLockingSupport
  with MatchmakingSupport {

  import java.util.UUID
  override val roomId: RoomId = UUID.randomUUID.toString

  val socketConfigurations: ConnectionConfigurations = ConnectionConfigurations.Default
  val autoClose: Boolean = true
  import scala.concurrent.duration._
  val autoCloseTimeout: FiniteDuration = 5 seconds

  protected var roomActor: Option[ActorRef] = None

  private var clients: Seq[Client] = Seq.empty
  // Clients that are allowed to reconnect with the associate expiration timer
  private var reconnectingClients: Seq[(Client, Timer)] = Seq.empty

  // By creating explicit setter, room actor can be kept "protected"
  def setRoomActor(actor: ActorRef): Unit = roomActor = Some(actor)

  /**
   * Add a client to the room. It triggers the onJoin handler
   *
   * @param client the client to add
   * @return true if the client successfully joined the room, false otherwise
   */
  def tryAddClient(client: Client, providedPassword: RoomPassword): Boolean = {
    val canJoin = checkPasswordCorrectness(providedPassword) && !isLocked && joinConstraints
    if (canJoin) {
      this.clients = client +: this.clients
      client send ProtocolMessage(JoinOk, client.id)
      this.onJoin(client)
    } else {
      client send ProtocolMessage(ClientNotAuthorized)
    }
    canJoin
  }

  /**
   * Reconnect the client to the room.
   *
   * @param client the client that wants to reconnect
   * @return true if the client successfully reconnected to the room, false otherwise
   */
  def tryReconnectClient(client: Client): Boolean = {
    val reconnectingClient = this.reconnectingClients.find(_._1.id == client.id)
    if (reconnectingClient.nonEmpty) {
      reconnectingClient.get._2.stopTimer()
      this.reconnectingClients = this.reconnectingClients.filter(_._1.id != client.id)
      this.clients = client +: this.clients
      client.send(ProtocolMessage(JoinOk, client.id))
    } else {
      client.send(ProtocolMessage(ClientNotAuthorized, client.id))
    }
    reconnectingClient.nonEmpty
  }

  /**
   * Allow the given client to reconnect to this room within the specified amount of time
   *
   * @param client the reconnecting client
   * @param period time in seconds within which the client can reconnect
   */
  def allowReconnection(client: Client, period: Long): Unit = {
    val timer = Timer()
    this.reconnectingClients = (client, timer) +: this.reconnectingClients

    timer.scheduleOnce(() => {
      this.reconnectingClients = this.reconnectingClients.filter(_._1.id != client.id)
    }, period * 1000) //seconds to millis
  }

  /**
   *
   * @return true if the autoclose operations need to start
   */
  def checkAutoClose(): Boolean = this.autoClose && this.connectedClients.isEmpty


  /**
   * Custom room constraints that may cause a join request to fail.
   *
   * @return true if the join request should be satisfied, false otherwise
   */
  def joinConstraints: Boolean = true

  /**
   * Remove a client from the room. Triggers onLeave
   *
   * @param client the client that leaved
   */
  def removeClient(client: Client): Unit = {
    this.clients = this.clients.filter(_.id != client.id)
    this.onLeave(client)
    client send ProtocolMessage(LeaveOk)
    if (this.checkAutoClose()) {
      this.roomActor.foreach(_ ! StartAutoCloseTimeout)
    }
  }

  /**
   *
   * @param client the client to check
   * @return true if the client is authorized to perform actions on this room
   */
  def clientAuthorized(client: Client): Boolean = connectedClients contains client

  /**
   * @return the list of connected clients
   */
  def connectedClients: Seq[Client] = this.clients

  /**
   * Send a message to a single client
   *
   * @param client  the client that will receive the message
   * @param message the message to send
   */
  def tell(client: Client, message: SocketSerializable): Unit =
    this.clients.filter(_.id == client.id).foreach(_.send(ProtocolMessage(ProtocolMessageType.Tell, client.id, message)))

  /**
   * Broadcast a message to all clients connected
   *
   * @param message the message to send
   */
  def broadcast(message: SocketSerializable): Unit =
    this.clients.foreach(client => client.send(ProtocolMessage(ProtocolMessageType.Broadcast, client.id, message)))

  /**
   * Close this room
   */
  def close(): Unit = {
    this.lock()
    this.clients.foreach(client => client.send(ProtocolMessage(ProtocolMessageType.RoomClosed, client.id)))
    this.roomActor.foreach(_ ! Close)
    this.onClose()
  }

  /**
   * Getter of all room properties
   *
   * @return a set containing all defined room properties
   */
  override def properties: Set[RoomProperty] = {
    def checkAdmissibleFieldType[T](value: T): Boolean = value match {
      case _: Int | _: String | _: Boolean | _: Double => true
      case _ => false
    }

    this.getClass.getDeclaredFields.filter(isProperty) collect {
      case f if operationOnField(f.getName)(field => checkAdmissibleFieldType(this --> field)) => propertyOf(f.getName)
    } toSet
  }

  override def valueOf(propertyName: String): Any = operationOnProperty(propertyName)(this --> _)

  /**
   * Getter of the value of a property
   * @param propertyName The name of the property
   * @throws NoSuchPropertyException if the requested property does not exist
   * @return The value of the property, expressed as a RoomPropertyValue
   */
  def `valueOf~AsPropertyValue`(propertyName: String): RoomPropertyValue =
    operationOnProperty(propertyName)(f => RoomPropertyValue propertyValueFrom (this --> f))

  override def propertyOf(propertyName: String): RoomProperty =
    operationOnProperty(propertyName)(f => RoomProperty(propertyName, RoomPropertyValue propertyValueFrom (this --> f)))

  /**
   * Setter of room properties
   *
   * @param properties A set containing the properties to set
   */
  def properties_=(properties: Set[RoomProperty]): Unit =
    properties.filter(p => try { isProperty(this fieldFrom p.name) } catch { case _: NoSuchFieldException => false })
      .map(ServerRoom.propertyToPair)
      .foreach(property => operationOnField(property.name)(f => f set(this, property.value)))

  /**
   * Called as soon as the room is created by the server
   */
  def onCreate(): Unit

  /**
   * Called as soon as the room is closed
   */
  def onClose(): Unit

  /**
   * Called when a user joins the room
   *
   * @param client tha user that joined
   */
  def onJoin(client: Client): Unit

  /**
   * Called when a user left the room
   *
   * @param client the client that left
   */
  def onLeave(client: Client): Unit

  /**
   * Called when the room receives a message
   *
   * @param client  the client that sent the message
   * @param message the message received
   */
  def onMessageReceived(client: Client, message: Any)

  /**
   * Tries to perform an operation on a property. If the property exists, the operation will be successfully completed,
   *  otherwise an error will be notified.
   * @param propertyName the name of the property
   * @param f            the operation to execute on such property
   * @tparam T           the type of return of the operation to execute
   * @throws NoSuchPropertyException if the requested property does not exist
   * @return             it return whatever the given function returns
   */
  private def operationOnProperty[T](propertyName: String)(f: Field => T): T = try {
    if (isProperty(this fieldFrom propertyName)) {
      operationOnField(propertyName)(f)
    } else {
      throw NoSuchPropertyException()
    }
  } catch {
    case _: NoSuchFieldException => throw NoSuchPropertyException()
  }

  /**
   * Perform an operation on a given field.
   *
   * @param fieldName the name of the field to use
   * @param f         the operation to execute, expressed as a function
   * @tparam T the type of return of the operation to execute
   * @throws NoSuchFieldException if the requested field doe not exist
   * @return it returns whatever the given function f returns
   */
  private def operationOnField[T](fieldName: String)(f: Field => T): T = {
    val field = this fieldFrom fieldName
    field setAccessible true
    val result = f(field)
    field setAccessible false
    result
  }

  private def fieldFrom(fieldName: String): Field = {
    this.getClass getDeclaredField fieldName
  }

  private def -->(field: Field): AnyRef = field get this // Get the value of the field

  private def isProperty(field: Field): Boolean =
    field.getDeclaredAnnotations collectFirst { case ann: RoomPropertyMarker => ann } nonEmpty
}

object ServerRoom {

  /**
   * It creates a SharedRoom from a given ServerRoom.
   * Properties of the basic ServerRoom are dropped (except for the private state),
   * just properties of the custom room are kept.
   *
   * @tparam T type of the room that extends ServerRoom
   * @return the created SharedRoom
   */
  implicit def serverRoomToSharedRoom[T <: ServerRoom]: T => SharedRoom = room => {

    // Calculate properties of the room
    var runtimeOnlyProperties = propertyDifferenceFrom(room)

    // Edit properties if the room uses game loop and/or synchronized state functionality
    if (room.isInstanceOf[GameLoop]) {
      val gameLoopOnlyPropertyNames = GameLoop.defaultProperties.map(_ name)
      runtimeOnlyProperties = runtimeOnlyProperties.filterNot(gameLoopOnlyPropertyNames contains _.name)
    }
    if (room.isInstanceOf[SynchronizedRoomState[_]]) {
      val syncStateOnlyPropertyNames = SynchronizedRoomState.defaultProperties.map(_ name)
      runtimeOnlyProperties = runtimeOnlyProperties.filterNot(syncStateOnlyPropertyNames contains _.name)
    }

    // Add public/private state to room properties
    import common.room.RoomPropertyValueConversions._
    runtimeOnlyProperties = runtimeOnlyProperties + RoomProperty(Room.RoomPrivateStatePropertyName, room.isPrivate)

    SharedRoom(room.roomId, runtimeOnlyProperties)
  }

  /**
   * Converter of sequence of room from ServerRoom to SharedRoom.
   *
   * @tparam T type of custom rooms that extend ServerRoom
   * @return A sequence of SharedRoom, where each element is the corresponding one mapped from the input sequence
   */
  implicit def serverRoomSeqToSharedRoomSeq[T <: ServerRoom]: Seq[T] => Seq[SharedRoom] = _.map(serverRoomToSharedRoom)

  /**
   * From a given room, it calculates properties not in common with a basic server room.
   * Useful for calculating just properties of a custom room, without the one of the basic one.
   *
   * @param runtimeRoom the room with its own custom properties
   * @return the set of property of the custom room that are not shared with the basic server room
   */
  def propertyDifferenceFrom[T <: ServerRoom](runtimeRoom: T): Set[RoomProperty] = {
    val serverRoomProperties = ServerRoom.defaultProperties
    val runtimeProperties = runtimeRoom.properties
    val runtimeOnlyPropertyNames = runtimeProperties.map(_ name) &~ serverRoomProperties.map(_ name)
    runtimeProperties.filter(property => runtimeOnlyPropertyNames contains property.name)
  }

  /**
   * A room with empty behavior
   */
  private case class BasicServerRoom(automaticClose: Boolean) extends ServerRoom {
    override val autoClose: Boolean = this.automaticClose

    override def onCreate(): Unit = {}

    override def onClose(): Unit = {}

    override def onJoin(client: Client): Unit = {}

    override def onLeave(client: Client): Unit = {}

    override def onMessageReceived(client: Client, message: Any): Unit = {}

    override def joinConstraints: Boolean = true
  }

  /**
   * Creates a simple server room with an empty behavior.
   *
   * @return the room
   */
  def apply(autoClose: Boolean = false): ServerRoom = BasicServerRoom(autoClose)

  /**
   * Getter of the default room properties defined in a server room
   *
   * @return a set containing the defined properties
   */
  def defaultProperties: Set[RoomProperty] = ServerRoom().properties // Create an instance of ServerRoom and get properties

  private case class PairRoomProperty[T](name: String, value: T)

  private def propertyToPair[_](property: RoomProperty): PairRoomProperty[_] =
    PairRoomProperty(property.name, RoomPropertyValue valueOf property.value)
}

