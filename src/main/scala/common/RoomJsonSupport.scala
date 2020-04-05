package common

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import common.SharedRoom.{Room, RoomId}
import spray.json.{DefaultJsonProtocol, JsBoolean, JsNumber, JsObject, JsString, JsValue, RootJsonFormat, deserializationError}

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


  // ____________________________________________________________________________________________________________________

  // Room property values
  implicit val intRoomPropertyJsonFormat: RootJsonFormat[IntRoomPropertyValue] = jsonFormat1(IntRoomPropertyValue)
  implicit val stringRoomPropertyJsonFormat: RootJsonFormat[StringRoomPropertyValue] = jsonFormat1(StringRoomPropertyValue)
  implicit val booleanRoomPropertyJsonFormat: RootJsonFormat[BooleanRoomPropertyValue] = jsonFormat1(BooleanRoomPropertyValue)

  implicit val roomPropertyValueJsonFormat: RootJsonFormat[RoomPropertyValue] = new RootJsonFormat[RoomPropertyValue] {

    def write(v: RoomPropertyValue): JsValue = JsObject("value" -> (v match {
      case p: IntRoomPropertyValue => intRoomPropertyJsonFormat write p
      case p: StringRoomPropertyValue => stringRoomPropertyJsonFormat write p
      case p: BooleanRoomPropertyValue => booleanRoomPropertyJsonFormat write p
    }))

    def read(value: JsValue): RoomPropertyValue = value match {
      case json: JsObject =>
        val value = json.fields("value").asJsObject
        value.fields("value") match {
          case _: JsNumber => value.convertTo[IntRoomPropertyValue]
          case _: JsString => value.convertTo[StringRoomPropertyValue]
          case _: JsBoolean => value.convertTo[BooleanRoomPropertyValue]
          case _ => deserializationError("Unknown room property value")
        }
      case _ => deserializationError("Room property value deserialization error")
    }
  }

  // Room property
  implicit val roomPropertyJsonFormat: RootJsonFormat[RoomProperty] = jsonFormat2(RoomProperty)

  // Filter Strategy
  implicit val equalStrategyJsonFormat: RootJsonFormat[EqualStrategy] = createStrategyJsonFormat(EqualStrategy())
  implicit val notEqualStrategyJsonFormat: RootJsonFormat[NotEqualStrategy] = createStrategyJsonFormat(NotEqualStrategy())
  implicit val greaterStrategyJsonFormat: RootJsonFormat[GreaterStrategy] = createStrategyJsonFormat(GreaterStrategy())
  implicit val lowerStrategyJsonFormat: RootJsonFormat[LowerStrategy] = createStrategyJsonFormat(LowerStrategy())

  private def createStrategyJsonFormat[T <: FilterStrategy](strategyType: T): RootJsonFormat[T] = new RootJsonFormat[T] {
    override def read(json: JsValue): T = json match {
      case JsString(_) => strategyType
      case _ => deserializationError(s"Strategy $strategyType deserialization error")
    }

    override def write(obj: T): JsValue = JsString(strategyType.name)
  }

  implicit val filterStrategy: RootJsonFormat[FilterStrategy] = new RootJsonFormat[FilterStrategy] {
    override def write(obj: FilterStrategy): JsValue = obj match {
      case s: EqualStrategy => equalStrategyJsonFormat write s
      case s: NotEqualStrategy => notEqualStrategyJsonFormat write s
      case s: GreaterStrategy => greaterStrategyJsonFormat write s
      case s: LowerStrategy => lowerStrategyJsonFormat write s
    }

    override def read(json: JsValue): FilterStrategy = json match {
      case JsString(name) if name == EqualStrategy().name => EqualStrategy()
      case JsString(name) if name == NotEqualStrategy().name => NotEqualStrategy()
      case JsString(name) if name == GreaterStrategy().name => GreaterStrategy()
      case JsString(name) if name == LowerStrategy().name => LowerStrategy()
    }
  }

  // Filter options
  implicit val filterOptionJsonFormat: RootJsonFormat[FilterOption] = jsonFormat3(FilterOption)
  implicit val filterOptionsJsonFormat: RootJsonFormat[FilterOptions] = new RootJsonFormat[FilterOptions] {
    override def write(obj: FilterOptions): JsValue = obj.options.map(filterOptionJsonFormat write).toJson

    override def read(json: JsValue): FilterOptions = FilterOptions(json.convertTo[Seq[FilterOption]])
  }
}
