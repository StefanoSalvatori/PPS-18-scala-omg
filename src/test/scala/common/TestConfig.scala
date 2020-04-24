package common


trait TestConfig {

  import scala.concurrent.duration.Duration
  import scala.concurrent.duration._
  import akka.util.Timeout

  val ServerLaunchAwaitTime: Duration = 10 seconds
  val ServerShutdownAwaitTime: Duration = 10 seconds
  implicit val DefaultTimeout: Timeout = 5 seconds
  implicit val DefaultDuration: Duration = 5 seconds

  val GameServerSpecServerPort = 8080
  val ClientSpecServerPort = 8081
  val HttpClientSpecServerPort = 8082
  val ServerActorSpecPort = 8083
  val CoreClientSpecServerPort = 8084
  val SocketHandlerSpecServerPort = 8085
  val ClientRoomActorSpecServerPort = 8086
  val ClientRoomSpecServerPort = 8087
  val MatchmakingSpecServerPort = 8088
  val ClientMatchmakingSpecServerPort = 8089


}
