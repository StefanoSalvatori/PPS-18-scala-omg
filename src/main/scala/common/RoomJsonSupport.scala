package common

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import client.room.ClientRoom.ClientRoom
import common.SharedRoom.{Room, RoomId}
import spray.json.{DefaultJsonProtocol, JsArray, JsBoolean, JsNumber, JsObject, JsString, JsValue, JsonFormat, JsonReader, JsonWriter, RootJsonFormat, deserializationError}
import common.BasicRoomPropertyValueConversions._

import scala.collection.immutable.Map

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

  // ____________________________________________________________________________________________________________________

  // Basic room property values
  implicit val intRoomPropertyJsonFormat: RootJsonFormat[IntRoomPropertyValue] = jsonFormat(IntRoomPropertyValue, "value")
  implicit val stringRoomPropertyJsonFormat: RootJsonFormat[StringRoomPropertyValue] = jsonFormat(StringRoomPropertyValue, "value")
  implicit val booleanRoomPropertyJsonFormat: RootJsonFormat[BooleanRoomPropertyValue] = jsonFormat(BooleanRoomPropertyValue, "value")

  // Room property
  implicit val roomPropertyJsonFormat: RootJsonFormat[RoomProperty] = new RootJsonFormat[RoomProperty] {

    def write(a: RoomProperty): JsValue = JsObject("name" -> JsString(a.name), "value" -> (a.value match {
      case p: IntRoomPropertyValue => intRoomPropertyJsonFormat write p
      case p: StringRoomPropertyValue => stringRoomPropertyJsonFormat write p
      case p: BooleanRoomPropertyValue => booleanRoomPropertyJsonFormat write p
    }))

    def read(value: JsValue): RoomProperty = value match {
      case json: JsObject =>
        val value = json.fields("value").asJsObject
        RoomProperty(json.fields("name").convertTo[String], value.fields("value") match {
          case _: JsNumber => value.convertTo[IntRoomPropertyValue]
          case _: JsString => value.convertTo[StringRoomPropertyValue]
          case _: JsBoolean => value.convertTo[BooleanRoomPropertyValue]
          case _ => deserializationError("Unknown room property value")
        })
      case _ => deserializationError("Room property deserialization error")
    }
  }

  // Filter Strategy
  implicit val equalStrategyJsonFormat: RootJsonFormat[EqualStrategy] = createStrategyJsonFormat(EqualStrategy())
  implicit val notEqualStrategyJsonFormat: RootJsonFormat[NotEqualStrategy] = createStrategyJsonFormat(NotEqualStrategy())
  implicit val greaterStrategyJsonFormat: RootJsonFormat[GreaterStrategy] = createStrategyJsonFormat(GreaterStrategy())
  implicit val lowerStrategyJsonFormat: RootJsonFormat[LowerStrategy] = createStrategyJsonFormat(LowerStrategy())

  private def createStrategyJsonFormat[T](strategyType: T): RootJsonFormat[T] = new RootJsonFormat[T] {
    override def read(json: JsValue): T = json match {
      case JsArray.empty => strategyType
      case _ => deserializationError(s"Strategy $strategyType deserialization error")
    }

    override def write(obj: T): JsValue = JsArray.empty
  }

  // filter options
  implicit val filterOptionsJsonFormat: RootJsonFormat[FilterOptions] = new RootJsonFormat[FilterOptions] {
    override def read(json: JsValue): FilterOptions = ???

    override def write(obj: FilterOptions): JsValue = ???
  }
}
