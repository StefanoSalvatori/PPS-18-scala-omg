package server

import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.http.scaladsl.server.Route

trait GameServerRoutes {
  val BASE_PATH = "gameserver"
  val serverRoutes: Route =
    path(this.BASE_PATH) {
      get {
        complete("Hello from server")
      }
    }

}
