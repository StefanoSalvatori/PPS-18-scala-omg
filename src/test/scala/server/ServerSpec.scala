package server

import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContextExecutor

class ServerSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest with RouteService {

  implicit val execContext: ExecutionContextExecutor = system.dispatcher

  behavior of "Server routing"

  it should "return a greeting message 'Hello' for GET requests to the root path" in {
    Get() ~> route ~> check {
      if(status.isSuccess) responseAs[String] shouldEqual "Hello"
    }
  }
}
