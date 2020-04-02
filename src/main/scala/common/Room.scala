package common

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, JsArray, JsString, JsValue, RootJsonFormat}

object CommonRoom {

  type RoomId = String
  type RoomType = String

  trait RoomState[T] {
    this: Room =>
    val state: T
  }

  trait Room {
    val roomId: RoomId
    // val roomType: String
  }


  object Room {
    def apply(id: RoomId): Room = new Room {
      override val roomId: RoomId = id
    }
  }
}







