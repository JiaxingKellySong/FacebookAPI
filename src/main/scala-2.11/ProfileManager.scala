import Server.{getProfile, setProfile}
import akka.actor.Actor
import data.{Keys, Profile}
import node.Profile

import scala.compat.Platform

/**
 * Created by jiaxing song on 2015/11/20.
 * Find the page and update the profile
 */
class ProfileManager extends Actor {

  override def receive = {
    case setProfile(id:Int,
        name:String,
        workplace:String,
        school:String,
        email:String,
        current_place:String,
        iv : String,
        keyMap : String,
        token : String) => {
      val clientID = Keys.tokenMap.getOrElse(token, -1)

      // update the access token time out map
      val nonTimeout =
        (Platform.currentTime - Keys.tokenTimeoutMap.getOrElse(token, 0).asInstanceOf[Long])  < Keys.TIME_OUT
      if (nonTimeout) { Keys.tokenTimeoutMap.put(token, Platform.currentTime) }

      if (clientID != id || !nonTimeout) {
        sender ! "false"
      } else {
        var page = Page.pagesMap.get(id).orNull
        var profile = new Profile(name)
        page.page_name_=(name)
        profile.workplace_=(workplace)
        profile.school_=(school)
        profile.email_=(email)
        profile.current_place_=(current_place)
        profile.iv_=(iv)
        profile.key_map_=(keyMap)
        page.profile_=(profile)
        sender ! "true"
      }
    }

    case getProfile(page_id : Int, token : String) =>
      val clientID = Keys.tokenMap.getOrElse(token, -1)
      val nonTimeout =
        (Platform.currentTime - Keys.tokenTimeoutMap.getOrElse(token, 0).asInstanceOf[Long])  < Keys.TIME_OUT
      if (nonTimeout) { Keys.tokenTimeoutMap.put(token, Platform.currentTime) }

      val page = Page.pagesMap.getOrElse(page_id, null)
      var profile : Profile = null
      if (page != null && nonTimeout)  { profile = page.profile }
      var map : Map[Int, String] = null
      if (profile != null) { map = Util.stringToMap(profile.key_map) }
      if (map != null) {
        val encryptedSymmetricKey = map.getOrElse(clientID, null)
        if (encryptedSymmetricKey != null) {
          Page.pagesMap.get(page_id).orNull.profile.encrypted_key_=(encryptedSymmetricKey)
          sender ! Profile.toJson(Page.pagesMap.get(page_id).orNull.profile)
        } else {
          sender ! "Permission Denied"
        }
      } else { sender ! "Permission Denied" }
  }
}
