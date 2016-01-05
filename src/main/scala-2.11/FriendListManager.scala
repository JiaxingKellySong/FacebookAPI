import Server.{addFriend, deleteFriend}
import akka.actor.Actor
import data.Keys

class FriendListManager extends Actor {
  override def receive: Receive = {
    case addFriend(id1, id2) => {
      if (Page.pagesMap.get(id1).orNull != null
        && !Page.pagesMap.get(id1).orNull.friendlist.idList.contains(id2)
        && Page.pagesMap.get(id2).orNull != null) {

        val friendList1 = Page.pagesMap.get(id1).orNull.friendlist.idList
        val friendList2 = Page.pagesMap.get(id2).orNull.friendlist.idList
        Page.pagesMap.get(id1).orNull.friendlist.idList = friendList1.::(id2)
        Page.pagesMap.get(id2).orNull.friendlist.idList = friendList2.::(id1)
        Page.pagesMap.get(id1).orNull.friendlist.listSize += 1
        Page.pagesMap.get(id2).orNull.friendlist.listSize += 1

        val keyList1 = Page.pagesMap.get(id1).orNull.keyList
        val keyList2 = Page.pagesMap.get(id2).orNull.keyList
        val key1 = Util.publicKeyToString(Keys.keyMap.get(id1).orNull)
        val key2 = Util.publicKeyToString(Keys.keyMap.get(id2).orNull)
        Page.pagesMap.get(id1).orNull.keyList = keyList1.::(key2)
        Page.pagesMap.get(id2).orNull.keyList = keyList2.::(key1)

        sender ! "{\"success\" : \"true\"}"
      } else {
        sender ! "{\"success\" : \"false\"}"
      }
    }
    case deleteFriend(id1, id2) => {
      if (Page.pagesMap.get(id1).orNull != null
        && Page.pagesMap.get(id1).orNull.friendlist.idList.contains(id2)
        && Page.pagesMap.get(id2).orNull != null) {

        val friendList1 = Page.pagesMap.get(id1).orNull.friendlist.idList
        val friendList2 = Page.pagesMap.get(id2).orNull.friendlist.idList
        Page.pagesMap.get(id1).orNull.friendlist.idList = friendList1.filter(_ != id2)
        Page.pagesMap.get(id2).orNull.friendlist.idList = friendList2.filter(_ != id1)
        Page.pagesMap.get(id1).orNull.friendlist.listSize -= 1
        Page.pagesMap.get(id2).orNull.friendlist.listSize -= 1

        val keyList1 = Page.pagesMap.get(id1).orNull.keyList
        val keyList2 = Page.pagesMap.get(id2).orNull.keyList
        Page.pagesMap.get(id1).orNull.keyList = keyList1.filter(_ != id2)
        Page.pagesMap.get(id2).orNull.keyList = keyList2.filter(_ != id1)
      }
    }
  }
}
