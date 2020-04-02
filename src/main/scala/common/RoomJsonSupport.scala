package common

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
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
