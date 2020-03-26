package client

object MessageDictionary {

  case class CreatePublicRoom()
  case class JoinedRoom(roomId: String)
  case class UnknownMessageReply()
}
