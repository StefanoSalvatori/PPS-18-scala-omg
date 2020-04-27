package server.room

trait PrivateRoomSupport {

  import common.room.Room
  import common.room.Room.RoomPassword
  private var password: RoomPassword = Room.DefaultPublicPassword

  /**
   * Check if the room is private.
   *
   * @return true if the room is private, false if it's public
   */
  def isPrivate: Boolean = password != Room.DefaultPublicPassword

  /**
   * It makes the room public.
   */
  def makePublic(): Unit = password = Room.DefaultPublicPassword

  /**
   * It makes the room private
   *
   * @param newPassword the password to be used to join the room
   */
  def makePrivate(newPassword: RoomPassword): Unit = password = newPassword

  /**
   * It checks if a provided password is the correct one.
   *
   * @param providedPassword the password provided, supposedly by a client
   * @return true if the password is correct or if the room is public, false otherwise.
   */
  protected def checkPasswordCorrectness(providedPassword: RoomPassword): Boolean =
    password == Room.DefaultPublicPassword || password == providedPassword
}

trait RoomLockingSupport {

  private var _isLocked = false

  /**
   * It checks if the room is currently locked.
   *
   * @return true if the room is locked, false otherwise
   */
  def isLocked: Boolean = _isLocked

  /**
   * It locks the room; it has no effect if the room was already locked.
   */
  def lock(): Unit = _isLocked = true

  /**
   * It unlocks the room; it has no effect if the room was already unlocked.
   */
  def unlock(): Unit = _isLocked = false
}

trait MatchmakingSupport {

  import server.matchmaking.Group.GroupId
  private var _matchmakingGroups: Map[Client, GroupId] = Map.empty

  /**
   * It sets the defined matchmaking groups.
   * @param groups groups that can be considered fair
   */
  def matchmakingGroups_=(groups: Map[Client, GroupId]): Unit = _matchmakingGroups = groups

  /**
   * Getter of the matchmaking groups
   * @return a map containing the group associated to each client
   */
  def matchmakingGroups: Map[Client, GroupId] = _matchmakingGroups

  /**
   * It checks if matchmaking is enabled in this room, namely if matchmaking groups are defined.
   * @return true if some groups are defined, false otherwise
   */
  def isMatchmakingEnabled: Boolean = _matchmakingGroups.nonEmpty
}
