package server.room

import common.room.SharedRoom.Room
import common.communication.CommunicationProtocol.{ProtocolMessageType, RoomProtocolMessage}
import common.room.{BooleanRoomPropertyValue, DoubleRoomPropertyValue, IntRoomPropertyValue, RoomProperty, RoomPropertyValue, StringRoomPropertyValue}
import common.communication.CommunicationProtocol.ProtocolMessageType._

trait PrivateRoomSupport {

  private val defaultPublicPassword: String = ""
  private var password: String = defaultPublicPassword

  def isPrivate: Boolean = password != defaultPublicPassword

  def makePublic(): Unit = password = defaultPublicPassword

  def makePrivate(newPassword: String): Unit = password = newPassword
}

trait ServerRoom extends Room with PrivateRoomSupport {

  private var clients: Seq[Client] = Seq.empty

  import java.lang.reflect.Field
  /**
   * Getter of the value of a property
   *
   * @param propertyName The name of the property
   * @return The value of the property, as instance of first class values (int, string, boolean. double)
   */
  def valueOf(propertyName: String): Any =
    operationOnField(propertyName)(field => field get this)

  /**
   * Getter of the value of a property
   *
   * @param propertyName The name of the property
   * @return The value of the property, expressed as a RoomPropertyValue
   */
  def `valueOf~AsProperty`(propertyName: String): RoomPropertyValue =
    operationOnField(propertyName)(field => ServerRoom.valueToRoomPropertyValue(field get this))

  /**
   * Gettor of a room property
   *
   * @param propertyName The name of the property
   * @return The selected property
   */
  def propertyOf(propertyName: String): RoomProperty =
    operationOnField(propertyName)(field => RoomProperty(propertyName, ServerRoom.valueToRoomPropertyValue(field get this)))

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
   * Add a client to the room. Triggers the onJoin
   *
   * @param client the client to add
   */
  def addClient(client: Client): Unit = {
    this.clients = client +: this.clients
    client.send(RoomProtocolMessage(JoinOk, client.id))
    this.onJoin(client)
  }

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
   * Close this room
   */
  def close(): Unit = {
    this.onClose()
    this.clients.foreach(c => c.send(RoomProtocolMessage(RoomClosed, c.id)))
  }

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
}

private case class PairRoomProperty[T](name: String, value: T)

object ServerRoom {

  /**
   * Creates a simple server room with an empty behavior.
   *
   * @param id the id of the room
   * @return the room
   */
  def apply(id: String): ServerRoom = new BasicServerRoom(id)

  private def propertyToPair[_](property: RoomProperty): PairRoomProperty[_] =
    PairRoomProperty(property.name, property.value match {
      case runtimeValue: IntRoomPropertyValue => runtimeValue.value
      case runtimeValue: StringRoomPropertyValue => runtimeValue.value
      case runtimeValue: BooleanRoomPropertyValue => runtimeValue.value
      case runtimeValue: DoubleRoomPropertyValue => runtimeValue.value
    })

  private def valueToRoomPropertyValue[T](value: T): RoomPropertyValue = value match {
    case v: Int => IntRoomPropertyValue(v)
    case v: String => StringRoomPropertyValue(v)
    case v: Boolean => BooleanRoomPropertyValue(v)
    case v: Double => DoubleRoomPropertyValue(v)
  }
}

private class BasicServerRoom(override val roomId: String) extends ServerRoom {
  override def onCreate(): Unit = {}

  override def onClose(): Unit = {}

  override def onJoin(client: Client): Unit = {}

  override def onLeave(client: Client): Unit = {}

  override def onMessageReceived(client: Client, message: Any): Unit = {}
}

