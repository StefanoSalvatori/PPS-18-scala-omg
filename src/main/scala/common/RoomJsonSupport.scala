package common

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import common.CommonRoom.{Room, RoomId}
import spray.json.{DefaultJsonProtocol, JsArray, JsString, JsValue, RootJsonFormat, deserializationError}
import common.BasicRoomPropertyValueConversions._

trait RoomJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val roomIdJsonFormat: RootJsonFormat[RoomId] = new RootJsonFormat[RoomId] {
    def write(a: RoomId): JsValue = JsString(a)

    import spray.json.deserializationError

    def read(value: JsValue): RoomId = value match {
      case JsString(roomId) => roomId
      case _ => deserializationError("id expected")
    }
  }
  implicit val simpleRoomJsonFormat: RootJsonFormat[Room] = new RootJsonFormat[Room] {
    def write(a: Room): JsValue = JsString(a.roomId)

    import spray.json.deserializationError

    def read(value: JsValue): Room = value match {
      case JsString(roomId) => Room(roomId)
      case _ => deserializationError("String id expected")
    }
  }


  //TODO: only works with RoomProperty[Any]
  implicit val simpleRoomPropertyJsonFormat: RootJsonFormat[RoomProperty] = new RootJsonFormat[RoomProperty] {
    def write(a: RoomProperty): JsValue = JsArray(JsString(a.name), JsString(a.value.toString))

    import spray.json.deserializationError
    def read(value: JsValue): RoomProperty = value match {
      case JsArray(Vector(JsString(roomId), JsString(value))) =>  RoomProperty(roomId, value)
      case _ => deserializationError("String id expected")
    }
  }


  implicit val simpleFilterOptionsJsonFormat: RootJsonFormat[FilterOptions] = new RootJsonFormat[FilterOptions] {
    def write(a: FilterOptions): JsValue = JsString("")

    def read(value: JsValue): FilterOptions = FilterOptions.empty()
  }
}
