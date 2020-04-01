package common

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}

trait Room <: {
  val roomId: String
}

case class SimpleRoomWithId(roomId: String) extends Room

case class RoomOptions(options: String)
case class RoomSeq(rooms: Seq[Room])

object Room {
  def apply(roomId: String): Room = SimpleRoomWithId(roomId)
}

trait RoomJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val roomOptJsonFormat: RootJsonFormat[RoomOptions]  = jsonFormat1(RoomOptions)
  implicit val roomIdJsonFormat: RootJsonFormat[SimpleRoomWithId]  = jsonFormat1(SimpleRoomWithId)
  implicit val roomSeqJsonFormat: RootJsonFormat[RoomSeq]  = jsonFormat1(RoomSeq)

  implicit val roomJsonFormat: RootJsonFormat[Room]  = new RootJsonFormat[Room] {
    def write(a: Room): JsValue = a match {
      case p: SimpleRoomWithId => roomIdJsonFormat.write(p)
    }

    def read(value: JsValue): SimpleRoomWithId =  value.convertTo[SimpleRoomWithId]
  }
}