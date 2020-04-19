package common.room

import common.room.RoomPropertyValueConversions._
import common.room.Room.SharedRoom
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RoomSpec extends AnyFlatSpec
  with Matchers
  with BeforeAndAfter {

  behavior of "Shared Room"

  it should "start with no properties at all" in {
    val room = SharedRoom("randomId", Set.empty[RoomProperty])
    assert(room.sharedProperties.isEmpty)
  }

  it should "add the right property" in {
    val n = 10
    val properties = (0 until n).map(i => RoomProperty(s"$i", i)).toSet
    val room = SharedRoom("RandomId", properties)
    room.sharedProperties should have size n
    (0 until n).foreach(i => assert(room.sharedProperties contains RoomProperty(s"$i", i)))
  }
}
