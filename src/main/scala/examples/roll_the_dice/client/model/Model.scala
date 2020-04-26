package examples.roll_the_dice.client.model

import client.Client

trait Model {

}

object Model {
  def apply(): Model = new ModelImpl()
}

class ModelImpl extends Model {

  import examples.roll_the_dice.common.ServerConfig._
  private val client = Client(host, port)

}
