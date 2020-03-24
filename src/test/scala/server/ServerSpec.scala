package server

import akka.http.scaladsl.model.{HttpEntity, HttpMethods, HttpRequest, MediaTypes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.ByteString
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContextExecutor

class ServerSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest {

  implicit val execContext: ExecutionContextExecutor = system.dispatcher

  behavior of "A Server"
}
