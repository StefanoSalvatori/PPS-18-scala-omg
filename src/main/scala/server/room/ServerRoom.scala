package server.room

import java.lang.reflect.Field
import java.util.UUID

import akka.actor.ActorRef
import com.typesafe.scalalogging.LazyLogging
import common.communication.CommunicationProtocol.ProtocolMessageType._
import common.communication.CommunicationProtocol.{ProtocolMessageType, RoomProtocolMessage}
import common.room.Room.{BasicRoom, RoomId, RoomPassword, SharedRoom}
import common.room._

trait PrivateRoomSupport {

  private var password: RoomPassword = Room.defaultPublicPassword

  def isPrivate: Boolean = password != Room.defaultPublicPassword

  def makePublic(): Unit = password = Room.defaultPublicPassword

  def makePrivate(newPassword: RoomPassword): Unit = password = newPassword

  def checkPasswordCorrectness(providedPassword: RoomPassword): Boolean = password == providedPassword
}

trait ServerRoom extends BasicRoom with PrivateRoomSupport with LazyLogging {

  override val roomId: RoomId = UUID.randomUUID.toString
  private var clients: Seq[Client] = Seq.empty
  private var closed = false

  protected var roomActor: ActorRef = _

  def setAssociatedActor(actor: ActorRef): Unit = roomActor = actor

  /**
   * Add a client to the room. Triggers the onJoin
   *
   * @param client the client to add
   * @return true if the client successfully joined the room, false otherwise
   */
  def tryAddClient(client: Client, providedPassword: RoomPassword): Boolean = {
    val canJoin = checkPasswordCorrectness(providedPassword) && joinConstraints
    if (canJoin) {
      this.clients = client +: this.clients
      client.send(RoomProtocolMessage(JoinOk, client.id))
      this.onJoin(client)
    }
    canJoin
  }

  /**
   * Custom room constraints that may cause a join request to fail.
   * @return true if the join request should be satisfied, false otherwise
   */
  def joinConstraints: Boolean

  /**
   * Remove a client from the room. Triggers onLeave
   *
   * @param client the client that leaved
   */
  def removeClient(client: Client): Unit = {
    this.clients = this.clients.filter(_.id != client.id)
    this.onLeave(client)
  }

  /**
   *
   * @param client the client to check
   * @return true if the client is authorized to perform actions on this room
   */
  def clientAuthorized(client: Client): Boolean = {
    this.connectedClients.contains(client)
  }

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
  def tell(client: Client, message: Any with java.io.Serializable): Unit =
    this.clients.filter(_.id == client.id).foreach(_.send(RoomProtocolMessage(ProtocolMessageType.Tell, client.id, message)))

  /**
   * Broadcast a message to all clients connected
   *
   * @param message the message to send
   */
  def broadcast(message: Any with java.io.Serializable): Unit =
    this.clients.foreach(client => client.send(RoomProtocolMessage(ProtocolMessageType.Broadcast, client.id, message)))

  /**
   *
   * @return true if this room is closed, false otherwise
   */
  def isClosed: Boolean = this.closed

  /**
   * Close this room
   */
  def close(): Unit = {
    this.closed = true
    this.clients.foreach(client => client.send(RoomProtocolMessage(ProtocolMessageType.RoomClosed, client.id)))
    this.onClose()
  }

  /**
   * Getter of all room properties
   *
   * @return a set containing all defined room properties
   */
  def properties: Set[RoomProperty] = {
    def checkAdmissibleFieldType[T](value: T): Boolean = value match {
      case _: Int | _: String | _: Boolean | _: Double => true
      case _ => false
    }

    this.getClass.getDeclaredFields.collect {
      case f if operationOnField(f.getName)(field => checkAdmissibleFieldType(this --> field)) => propertyOf(f.getName)
    }.toSet
  }

  override def valueOf(propertyName: String): Any = operationOnField(propertyName)(this --> _)

  /**
   * Getter of the value of a property
   *
   * @param propertyName The name of the property
   * @return The value of the property, expressed as a RoomPropertyValue
   */
  def `valueOf~AsPropertyValue`(propertyName: String): RoomPropertyValue = operationOnField(propertyName) { field =>
    RoomPropertyValue propertyValueFrom (this --> field)
  }

  override def propertyOf(propertyName: String): RoomProperty = operationOnField(propertyName) { field =>
    RoomProperty(propertyName, RoomPropertyValue propertyValueFrom (this --> field))
  }

  /**
   * Setter of room properties
   *
   * @param properties A set containing the properties to set
   */
  def setProperties(properties: Set[RoomProperty]): Unit = properties.map(ServerRoom.propertyToPair).foreach(property => {
    try {
      operationOnField(property.name)(f => f set(this, property.value))
    } catch {
      case _: NoSuchFieldException =>
        logger debug s"Impossible to set property '${property.name}': No such property in the room."
    }
  })

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

  private def operationOnField[T](fieldName: String)(f: Function[Field, T]): T = {
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
}

private case class PairRoomProperty[T](name: String, value: T)

object ServerRoom {
  implicit val serverRoomToSharedRoom: ServerRoom => SharedRoom = serverRoom => {
    // Create the shared room
    val sharedRoom = SharedRoom(serverRoom.roomId)
    // Set the shared room properties
    val serverRoomProperties = ServerRoom.defaultProperties
    val runtimeRoomProperties = serverRoom.properties
    var runtimeOnlyPropertyNames: Set[String] = runtimeRoomProperties.map(_ name) &~ serverRoomProperties.map(_ name)
    // Edit properties if the room uses game loop functionality
    if (serverRoom.isInstanceOf[GameLoop]) {
      runtimeOnlyPropertyNames = runtimeOnlyPropertyNames - "worldUpdateRate"
    }
    if (serverRoom.isInstanceOf[SynchronizedRoomState[_]]) {
      runtimeOnlyPropertyNames = runtimeOnlyPropertyNames - "stateUpdateRate"
    }
    // Add selected properties to the shared room
    runtimeRoomProperties.filter(property => runtimeOnlyPropertyNames contains property.name)
      .foreach(sharedRoom addSharedProperty)
    // Add the public/private state to room properties
    import common.room.RoomPropertyValueConversions._
    sharedRoom addSharedProperty RoomProperty(Room.roomPrivateStatePropertyName, serverRoom.isPrivate)
    sharedRoom
  }
  implicit val serverRoomSeqToSharedRoomSeq: Seq[ServerRoom] => Seq[SharedRoom] = _.map(serverRoomToSharedRoom)

  /**
   * Creates a simple server room with an empty behavior.
   *
   * @return the room
   */
  def apply(): ServerRoom = BasicServerRoom()

  /**
   * Getter of the default room properties defined in a server room
   *
   * @return a set containing the names of the defined properties
   */
  def defaultProperties: Set[RoomProperty] = ServerRoom().properties // Create an instance of ServerRoom and get properties

  private def propertyToPair[_](property: RoomProperty): PairRoomProperty[_] =
    PairRoomProperty(property.name, RoomPropertyValue valueOf property.value)
}

/**
 * A room with empty behavior
 */
private case class BasicServerRoom() extends ServerRoom {
  override def onCreate(): Unit = { }
  override def onClose(): Unit = { }
  override def onJoin(client: Client): Unit = { }
  override def onLeave(client: Client): Unit = { }
  override def onMessageReceived(client: Client, message: Any): Unit = { }
  override def joinConstraints: Boolean = true
}

