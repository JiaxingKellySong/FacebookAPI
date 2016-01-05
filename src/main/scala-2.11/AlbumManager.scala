import Server.{createAlbum, deleteAlbum, getAlbum}
import akka.actor.Actor
import data.{Album, Keys, Picture}
import node.Album

import scala.compat.Platform

/**
 * Created by jiaxing song on 2015/11/25.
 */
class AlbumManager extends Actor{

  def receive = {
    case createAlbum(album_id, creator_id, create_time, token) =>
      var album = new Album(album_id)
      album.creator_id_=(creator_id)
      album.create_time_=(create_time)

      Album.albumMap.put(Album.id, album)

    case getAlbum(album_id, token) =>
      val clientID = Keys.tokenMap.getOrElse(token, -1)
      // update the access token time out map
      val nonTimeout =
        (Platform.currentTime - Keys.tokenTimeoutMap.getOrElse(token, 0).asInstanceOf[Long])  < Keys.TIME_OUT
      if (nonTimeout) { Keys.tokenTimeoutMap.put(token, Platform.currentTime) }

      val album = Album.albumMap.getOrElse(album_id, null)
      if (album != null && nonTimeout) {
        val key_map = Util.stringToMap(album.key_map)
        val encrypted_key = key_map.getOrElse(clientID, null)
        if (encrypted_key != null) {
          sender ! Album.toJson(album)
        } else {
          sender ! "Permission Denied"
        }
      } else {
        sender ! "Permission Denied"
      }

    case deleteAlbum(album_id, token) =>
      val clientID = Keys.tokenMap.getOrElse(token, -1)

      // update the access token time out map
      val nonTimeout =
        (Platform.currentTime - Keys.tokenTimeoutMap.getOrElse(token, 0).asInstanceOf[Long])  < Keys.TIME_OUT
      if (nonTimeout) { Keys.tokenTimeoutMap.put(token, Platform.currentTime) }

      if (Album.albumMap.getOrElse(album_id, null) != null
        && clientID == Album.albumMap.getOrElse(album_id, null).creator_id
        && nonTimeout) {
        val album = Album.albumMap.get(album_id).orNull
        Album.albumMap.remove(album_id)
        // delete the picture in the album
        for(pic_id <- album.pic_id_list) {
          Picture.picMap.remove(pic_id)
        }
        sender ! "{\"success\" : \"true\"}"
      } else {
        sender ! "{\"success\" : \"false\"}"
      }
  }
}
