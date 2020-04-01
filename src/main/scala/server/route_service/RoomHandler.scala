package server.route_service

import common.{Room, RoomOptions, RoomPropertyValue, FilterOptions}
import server.room.RoomStrategy

trait RoomHandler {

  /**
   * @return the list of available rooms
   */
  def availableRooms: List[Room]

  /**
   * FIlter rooms using the specified filters.
   * @param filterOptions
   *     The filters to be applied
   * @return
   *     A set of rooms that satisfy the filters
   */
  def availableRooms(filterOptions: FilterOptions): Set[Room]

  /**
   * Create a new room of specific type
   *
   * @param roomType    room type
   * @param roomOptions room options
   * @return the created room
   */
  def createRoom(roomType: String, roomOptions: Option[RoomOptions]): Room

  /**
   * Get the list of available room of given type.
   * If no rooms exist for the given type creates a new room and
   * return a list containing only the create room.
   *
   * @param roomType    room type
   * @param roomOptions room options for creation
   * @return list of rooms
   */
  def getOrCreate(roomType: String, roomOptions: Option[RoomOptions]): List[Room]

  /**
   * Return a room with given type and id. Non if does not exists
   *
   * @param roomType room type
   * @param roomId   room id
   * @return option containg the rrom if present, non otherwise
   */
  def getRoomById(roomType: String, roomId: String): Option[Room]

  /**
   * All available rooms filtered by type
   *
   * @param roomType rooms type
   * @return the list of rooms of given type
   */
  def getRoomsByType(roomType: String): List[Room]

  /**
   * Define a new room type to handle on room creation.
   * New rooms with this type are created with the given room strategy
   *
   * @param roomType     room type
   * @param roomStrategy room strategy
   */
  def defineRoomType(roomType: String, roomStrategy: RoomStrategy)
}

object RoomHandler {
  def apply(): RoomHandler = RoomHandlerImpl()
}

case class RoomHandlerImpl() extends RoomHandler {

  var roomTypesHandlers: Map[String, RoomStrategy] = Map.empty

  //type1 ->  (id->room), (id2, room2) ...
  //type2 -> (id->room), (id2, room2) ...
  var roomsByType: Map[String, Map[String, Room]] = Map.empty

  override def availableRooms: List[Room] = roomsByType.values.flatMap(_.values).toList

  override def availableRooms(filterOptions: FilterOptions): Set[Room] =
    roomsByType.values.flatMap(_ values).filter(room => {

    // Given a room, check if such room satisfies all filter constraints
    filterOptions.options.forall(filterOption => {
      try {
        val field = room.getClass getDeclaredField filterOption.optionName

        field setAccessible true

        val value = (field get room).asInstanceOf[RoomPropertyValue]
        val filterValue = filterOption.value.asInstanceOf[value.type]

        field setAccessible false

        filterOption.strategy evaluate (value, filterValue)
      } catch {
        // A room is dropped if it doesn't contain the specified field to be used in the filter
        case _: NoSuchFieldException => false
      }
    })
  }).toSet

  override def createRoom(roomType: String, roomOptions: Option[RoomOptions]): Room = {
    this.handleRoomCreation(roomType, roomOptions)
  }

  override def getRoomsByType(roomType: String): List[Room] = this.roomsByType(roomType).values.toList

  override def getOrCreate(roomType: String, roomOptions: Option[RoomOptions]): List[Room] =
    this.roomsByType.get(roomType) match {
      case Some(r) => r.values.toList
      case None => List(createRoom(roomType, roomOptions))
    }

  override def getRoomById(roomType: String, roomId: String): Option[Room] =
    this.roomsByType(roomType).get(roomId)


  override def defineRoomType(roomTypeName: String, roomStrategy: RoomStrategy): Unit = {
    this.roomsByType = this.roomsByType + (roomTypeName -> Map.empty)
    this.roomTypesHandlers = this.roomTypesHandlers + (roomTypeName -> roomStrategy)
  }

  private def handleRoomCreation(roomType: String, roomOptions: Option[RoomOptions]): Room = {
    val roomMap = this.roomsByType(roomType)
    val newId = (roomMap.size + 1).toString
    val newRoom = Room(newId)
    val newRoomMap = roomMap + (newId -> newRoom)
    this.roomsByType = this.roomsByType.updated(roomType, newRoomMap)
    newRoom
  }
}