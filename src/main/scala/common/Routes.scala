package common

object Routes {

  def publicRooms: String = "rooms"

  def roomsByType(roomType: String): String = publicRooms + "/" +  roomType

  def roomByTypeAndId(roomType: String, roomId: String): String = roomsByType(roomType) + "/" + roomId

  def uri(address: String, port: Int): String = "http://" + address + ":" + port
}
