package server.room.socket

import scala.concurrent.duration._
import scala.concurrent.duration.{Duration, FiniteDuration}


object ConnectionConfigurations {
  val Default: ConnectionConfigurations = ConnectionConfigurations()
}

/**
 * Configuration values for room web sockets.
 *
 * @param idleConnectionTimeout time after which an idle connections  to a client should be closed
 * @param keepAlive             if set to a finite duration it will send heartbeat messages to client at the
 *                              specified rate. Duration.Inf means no heartbeat
 */
case class ConnectionConfigurations(idleConnectionTimeout: FiniteDuration = 60 seconds,
                                    keepAlive: Duration = Duration.Inf) {
  val isKeepAliveActive: Boolean = this.keepAlive != Duration.Inf
}
