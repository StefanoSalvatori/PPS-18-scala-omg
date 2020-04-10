package common.room

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import common.room.Room.{SharedRoom, RoomId}
import spray.json.{DefaultJsonProtocol, JsArray, JsBoolean, JsNumber, JsObject, JsString, JsValue, RootJsonFormat, deserializationError}

trait RoomJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  // Room
  implicit val roomIdJsonFormat: RootJsonFormat[RoomId] = new RootJsonFormat[RoomId] {
    def write(a: RoomId): JsValue = JsString(a)

    def read(value: JsValue): RoomId = value match {
      case JsString(roomId) => roomId
      case _ => deserializationError("id expected")
    }
  }

  implicit val sharedRoomJsonFormat: RootJsonFormat[SharedRoom] = new RootJsonFormat[SharedRoom] {

    private val idJsonPropertyName = "id"
    private val propertiesJsonPropertyName  ="properties"

    def write(a: SharedRoom): JsValue = JsObject(
      idJsonPropertyName -> JsString(a.roomId),
      propertiesJsonPropertyName -> roomPropertySetJsonFormat.write(a.sharedProperties)
    )

    def read(value: JsValue): SharedRoom = value match {
      case JsObject(json) =>
        json(idJsonPropertyName) match {
          case s: JsString =>
            val room = SharedRoom(s.value)
            json(propertiesJsonPropertyName).convertTo[Set[RoomProperty]].foreach(room addSharedProperty)
            room
          case _ => deserializationError("Error while reading shared room id")
        }
      case _ => deserializationError("Error while reading shared room")
    }
  }

  // Room property values
  implicit val intRoomPropertyJsonFormat: RootJsonFormat[IntRoomPropertyValue] = jsonFormat1(IntRoomPropertyValue)
  implicit val stringRoomPropertyJsonFormat: RootJsonFormat[StringRoomPropertyValue] = jsonFormat1(StringRoomPropertyValue)
  implicit val booleanRoomPropertyJsonFormat: RootJsonFormat[BooleanRoomPropertyValue] = jsonFormat1(BooleanRoomPropertyValue)
  implicit val doubleRoomPropertyJsonFormat: RootJsonFormat[DoubleRoomPropertyValue] = jsonFormat1(DoubleRoomPropertyValue)

  implicit val roomPropertyValueJsonFormat: RootJsonFormat[RoomPropertyValue] = new RootJsonFormat[RoomPropertyValue] {

    private val valueJsPropertyName = "value"

    def write(v: RoomPropertyValue): JsValue = JsObject(valueJsPropertyName -> (v match {
      case p: IntRoomPropertyValue => intRoomPropertyJsonFormat write p
      case p: StringRoomPropertyValue => stringRoomPropertyJsonFormat write p
      case p: BooleanRoomPropertyValue => booleanRoomPropertyJsonFormat write p
      case p: DoubleRoomPropertyValue => doubleRoomPropertyJsonFormat write p
    }))

    def read(value: JsValue): RoomPropertyValue = value match {
      case json: JsObject =>
        val value = json.fields(valueJsPropertyName).asJsObject
        value.fields(valueJsPropertyName) match {
          case runtimeValue: JsNumber =>
            if (runtimeValue.toString contains ".") {
              value.convertTo[DoubleRoomPropertyValue]
            } else {
              value.convertTo[IntRoomPropertyValue]
            }
          case _: JsString => value.convertTo[StringRoomPropertyValue]
          case _: JsBoolean => value.convertTo[BooleanRoomPropertyValue]
          case _ => deserializationError("Unknown room property value")
        }
      case _ => deserializationError("Room property value deserialization error")
    }
  }

  // Room property
  implicit val roomPropertyJsonFormat: RootJsonFormat[RoomProperty] = jsonFormat2(RoomProperty)
  implicit val roomPropertySetJsonFormat: RootJsonFormat[Set[RoomProperty]] = new RootJsonFormat[Set[RoomProperty]] {
    override def write(obj: Set[RoomProperty]): JsValue = obj.map(roomPropertyJsonFormat write).toJson

    override def read(json: JsValue): Set[RoomProperty] = json match {
      case JsArray(elements) => elements.map(_.convertTo[RoomProperty]).toSet
      case _ => deserializationError("Room property set deserialization error")
    }
  }

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
