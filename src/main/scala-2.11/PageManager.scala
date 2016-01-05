import Server.{createPage, deletePage, getPage}
import akka.actor.Actor
import data.Keys
import node.Page

import scala.compat.Platform


/**
 * Created by jiaxing song on 2015/11/19.
 * An actor for creating new pages
 */
class PageManager extends Actor{
  def receive = {

    case createPage(page_id : Int, name : String, keyStr : String) =>
      Page.pagesMap.put(page_id, new Page(page_id, name))
      val publicKey = Util.stringToPublicKey(keyStr)
      Keys.keyMap.put(page_id, publicKey)

    case getPage(page_id: Int, access_token: String) =>
      val clientID = Keys.tokenMap.getOrElse(access_token, -1)

      // update the access token time out map
      val nonTimeout =
        (Platform.currentTime - Keys.tokenTimeoutMap.getOrElse(access_token, 0).asInstanceOf[Long])  < Keys.TIME_OUT
      if (nonTimeout) { Keys.tokenTimeoutMap.put(access_token, Platform.currentTime) }

      //only friends can access the page
      if (clientID != -1
        && (clientID == page_id || Page.areFriend(clientID, page_id))
        && nonTimeout) {
        sender ! Page.toJson(Page.pagesMap.get(page_id).orNull)
      } else {
        sender ! "Permission Denied"
      }

    case deletePage(page_id: Int, access_token: String) =>
      val clientID = Keys.tokenMap.getOrElse(access_token, -1)

      val nonTimeout =
        (Platform.currentTime - Keys.tokenTimeoutMap.getOrElse(access_token, 0).asInstanceOf[Long])  < Keys.TIME_OUT
      if (nonTimeout) { Keys.tokenTimeoutMap.put(access_token, Platform.currentTime) }

      if (clientID != -1 && clientID == page_id) {
        Page.pagesMap -= page_id
      }

  }
}
