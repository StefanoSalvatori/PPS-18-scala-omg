package common

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import client.room.ClientRoom.{ClientRoom, ClientRoomImpl}
import spray.json.{DefaultJsonProtocol, JsValue, RootJsonFormat}
object CommonRoom {

  type RoomId = String
  type RoomType = String

  trait Room <: {
    val roomId: String
  }

  object Room {
    def apply(roomId: String): Room = SimpleRoomWithId(roomId)
  }


  case class SimpleRoomWithId(roomId: String) extends Room
  case class RoomOptions(options: String)
  case class RoomSeq(rooms: Seq[Room])


  trait RoomJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

    implicit val roomOptJsonFormat: RootJsonFormat[RoomOptions]  = jsonFormat1(RoomOptions)
    implicit val roomIdJsonFormat: RootJsonFormat[SimpleRoomWithId]  = jsonFormat1(SimpleRoomWithId)
    implicit val roomClientIdJsonFormat: RootJsonFormat[ClientRoomImpl]  = jsonFormat1(ClientRoomImpl)
    implicit val roomSeqJsonFormat: RootJsonFormat[RoomSeq]  = jsonFormat1(RoomSeq)

    implicit val roomJsonFormat: RootJsonFormat[Room]  = new RootJsonFormat[Room] {
      def write(a: Room): JsValue = a match {
        case p: SimpleRoomWithId => roomIdJsonFormat.write(p)
      }

      def read(value: JsValue): SimpleRoomWithId =  value.convertTo[SimpleRoomWithId]
    }

    implicit val clientToomJsonFormat: RootJsonFormat[ClientRoom]  = new RootJsonFormat[ClientRoom] {
      def write(a: ClientRoom): JsValue = a match {
        case p: ClientRoomImpl => roomClientIdJsonFormat.write(p)
      }

      def read(value: JsValue): ClientRoomImpl =  value.convertTo[ClientRoomImpl]
    }

  }
}
