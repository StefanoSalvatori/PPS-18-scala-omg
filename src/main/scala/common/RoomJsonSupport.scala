package common

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import client.room.ClientRoom.{ClientRoom, ClientRoomImpl}
import common.CommonRoom.Room
import spray.json.{DefaultJsonProtocol, JsArray, JsString, JsValue, RootJsonFormat}

trait RoomJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val simpleRoomJsonFormat: RootJsonFormat[Room] = new RootJsonFormat[Room] {
    def write(a: Room): JsValue = JsString(a.roomId)

    import spray.json.deserializationError

    def read(value: JsValue): Room = value match {
      case JsString(roomId) => Room(roomId)
      case _ => deserializationError("String id expected")
    }
  }

  implicit val clientRoomJsonFormat: RootJsonFormat[ClientRoom] = new RootJsonFormat[ClientRoom] {
    implicit val roomClientIdJsonFormat: RootJsonFormat[ClientRoomImpl]  = jsonFormat1(ClientRoomImpl)
    def write(a: ClientRoom): JsValue = a match {
      case p: ClientRoomImpl => roomClientIdJsonFormat.write(p)
    }

    def read(value: JsValue): ClientRoomImpl =  value.convertTo[ClientRoomImpl]
  }

  //TODO: only works with RoomProperty[Any]
  implicit val simpleRoomPropertyJsonFormat: RootJsonFormat[RoomProperty[Any]] = new RootJsonFormat[RoomProperty[Any]] {
    def write(a: RoomProperty[Any]): JsValue = JsArray(JsString(a.name), JsString(a.value.toString))

    import spray.json.deserializationError
    def read(value: JsValue): RoomProperty[Any] = value match {
      case JsArray(Vector(JsString(roomId), JsString(value))) =>
        RoomProperty(roomId, value).asInstanceOf[RoomProperty[Any]]
      case _ => deserializationError("String id expected")
    }
  }
}
