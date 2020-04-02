package common

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, JsArray, JsString, JsValue, RootJsonFormat}


trait RoomState[T] {
  this: Room =>
  val state: T
}

trait Room {
  val roomId: String
  // val roomType: String
}


object Room {
  def apply(id: String): Room = new Room {
    override val roomId: String = id
  }
}





