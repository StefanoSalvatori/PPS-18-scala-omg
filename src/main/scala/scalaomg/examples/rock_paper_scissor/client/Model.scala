package scalaomg.examples.rock_paper_scissor.client

import scalaomg.client.core.Client

object Model {
  var client: Client = _

  def init(host: String, port: Int): Unit = {
    this.client = Client(host, port)
  }
}
