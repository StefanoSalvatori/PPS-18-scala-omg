package common

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import client.room.ClientRoom.ClientRoom
import common.SharedRoom.{Room, RoomId}
import spray.json.{DefaultJsonProtocol, JsArray, JsString, JsValue, RootJsonFormat, deserializationError}
import common.BasicRoomPropertyValueConversions._

trait RoomJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val roomIdJsonFormat: RootJsonFormat[RoomId] = new RootJsonFormat[RoomId] {
    def write(a: RoomId): JsValue = JsString(a)

    def read(value: JsValue): RoomId = value match {
      case JsString(roomId) => roomId
      case _ => deserializationError("id expected")
    }
  }
  implicit val simpleRoomJsonFormat: RootJsonFormat[Room] = new RootJsonFormat[Room] {
    def write(a: Room): JsValue = JsString(a.roomId)

    def read(value: JsValue): Room = value match {
      case JsString(roomId) => Room(roomId)
      case _ => deserializationError("String id expected")
    }
  }

  implicit val clientRoomJsonFormat: RootJsonFormat[ClientRoom] = new RootJsonFormat[ClientRoom] {
    def write(a: ClientRoom): JsValue = JsString(a.roomId)

    def read(value: JsValue): ClientRoom =  value match {
      case JsString(roomId) =>  ClientRoom(roomId)
      case _ => deserializationError("String id expected")
    }
  }

  implicit val intRoomPropertyJsonFormat: RootJsonFormat[IntRoomPropertyValue] = new RootJsonFormat[IntRoomPropertyValue] {
    def read(json: JsValue): IntRoomPropertyValue = json match {
      case JsString(value) =>
        IntRoomPropertyValue(value.asInstanceOf[Int])
      case _ =>
        deserializationError("Int room property value deserialization error")
    }

    def write(obj: IntRoomPropertyValue): JsValue = JsString(obj.value.toString)
  }

  implicit val stringRoomPropertyJsonFormat: RootJsonFormat[StringRoomPropertyValue] = new RootJsonFormat[StringRoomPropertyValue] {
    def read(json: JsValue): StringRoomPropertyValue = json match {
      case JsString(value) =>
        StringRoomPropertyValue(value)
      case _ =>
        deserializationError("Int room property value deserialization error")
    }

    def write(obj: StringRoomPropertyValue): JsValue = JsString(obj.value)
  }

  implicit val booleanRoomPropertyJsonFormat: RootJsonFormat[BooleanRoomPropertyValue] = new RootJsonFormat[BooleanRoomPropertyValue] {
    def read(json: JsValue): BooleanRoomPropertyValue = json match {
      case JsString(value) =>
        BooleanRoomPropertyValue(value.asInstanceOf[Boolean])
      case _ =>
        deserializationError("Int room property value deserialization error")
    }

    def write(obj: BooleanRoomPropertyValue): JsValue = JsString(obj.value.toString)
  }

  //TODO: only works with RoomProperty[Any]
  implicit val simpleRoomPropertyJsonFormat: RootJsonFormat[RoomProperty] = new RootJsonFormat[RoomProperty] {
    def write(a: RoomProperty): JsValue = JsArray(JsString(a.name), JsString(a.value.toString))

    def read(value: JsValue): RoomProperty = value match {
      case JsArray(Vector(JsString(roomId), JsString(value))) =>
        RoomProperty(roomId, value)
      case _ => deserializationError("String id expected")
    }
  }

  /*
  implicit val equalFilterStrategyJsonFormat: RootJsonFormat[EqualStrategy] = new RootJsonFormat[EqualStrategy] {
    def read(json: JsValue): FilterStrategy = json match {
      case JsArray.empty =>
        EqualStrategy()
      case _ => deserializationError("Equal strategy deserialization error")

    }

    def write(obj: FilterStrategy): JsValue = JsArray.empty
  }

  implicit val filterOptionsJsonFormat : RootJsonFormat[FilterOption] = new RootJsonFormat[FilterOption] {
    def read(json: JsValue): FilterOption = json match {
      case JsArray(Vector(JsString(optionName), strategy, JsString(value))) =>
        FilterOption(optionName, strategy, value)
      case _ => deserializationError("Filter option deserialization error")
    }

    def write(obj: FilterOption): JsValue =
      JsArray(JsString(obj.optionName), JsString(obj.strategy.toString), JsString(obj.value.toString))
  }
  */
}
