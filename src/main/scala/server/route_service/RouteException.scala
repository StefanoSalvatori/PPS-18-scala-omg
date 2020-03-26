package server.route_service


sealed trait RouteException extends Throwable
case class RoomTypeNotFound() extends RouteException
case class RoomIdNotFound() extends RouteException
case class BadRequest() extends RouteException