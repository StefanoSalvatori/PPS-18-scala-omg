package server.room

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, JsValue, JsonWriter, RootJsonFormat}


/**
 * Simple room for testing
 */
final case class Room(roomId: Int, roomOpt: RoomOptions)
final case class RoomOptions(roomName: String, maxClients: Int)
final case class RoomSeq(rooms: Seq[Room])

trait RoomJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val roomOptionsJsonFormat: RootJsonFormat[RoomOptions]  = jsonFormat2(RoomOptions)
  implicit val roomJsonFormat: RootJsonFormat[Room]  = jsonFormat2(Room)
  implicit val roomSeqJsonFormat: RootJsonFormat[RoomSeq]  = jsonFormat1(RoomSeq)
}


