package server


trait GameServer {

  type Room = String

  /**
   * The host where the game server runs.
   */
  val host: String

  /**
   * The port the server is listening on.
   */
  val port: Int

  /**
   * Start the server listening at [[server.GameServer#host]]:[[server.GameServer#port]].
   */
  def start(): Unit

  /**
   * Shutdown the server.
   */
  def shutdown(): Unit

  /**
   * Add a new type of room to the server
   *
   * @param room The type of room to add
   */
  def defineRoom(room: Room)

}


object GameServer {
  /**
   * Create a new game server at the the specified host and port
   *
   * @param host the hostname of the server
   * @param port the port it will be listening on
   * @return an instance if a [[server.GameServer]]
   */
  def apply(host: String, port: Int): GameServer = new GameServerImpl(host, port)


  def fromExistingServer(akkaServer: Any): GameServer = _
}


private class GameServerImpl(override val host: String,
                             override val port: Int) extends GameServer {

  /**
   * Start the server listening at [[server.GameServer#host]]:[[server.GameServer#port]].
   */
  override def start(): Unit = {
    println("Starting server")
    println("Server is listening on " + host + ":" + port)
  }


  /**
   * Shutdown the server.
   */
  override def shutdown(): Unit = {
    println("Shutting down server")
    println("Server is down")

  }

  /**
   * Add a new type of room to the server
   *
   * @param room The type of room to add
   */
  override def defineRoom(room: Room): Unit = {
    println("Defined " + room)
  }
}


