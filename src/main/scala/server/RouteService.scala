package server

import akka.http.scaladsl.server.Directives.{complete, get}

/**
 * Specifies routes
 */
trait RouteService {
  val route =
    get {
      complete("Hello")
    }
}
