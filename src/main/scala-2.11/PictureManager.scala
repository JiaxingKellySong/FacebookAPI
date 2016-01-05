import Server.{createPicture, deletePicture, getPicture}
import akka.actor.Actor
import data.{Album, Keys, Picture}
import node.Picture

import scala.compat.Platform

/**
 * Created by jiaxing song on 2015/11/25.
 */
class PictureManager extends Actor{

  def receive = {
    case createPicture(pic_id, album_id, creator_id, create_time, picture_data, iv, token) =>

      var picture = new Picture(pic_id)
      picture.album_id_=(album_id)
      picture.creator_id_=(creator_id)
      picture.create_time_=(create_time)
      picture.picture_data_=(picture_data)
      Picture.picMap.put(Picture.id, picture)
      if (album_id != -1
          && Album.albumMap.contains(album_id)
          && Album.albumMap.get(album_id).orNull != null) {
          Album.albumMap.get(album_id).orNull.pic_id_list
            = Album.albumMap.get(album_id).orNull.pic_id_list.::(Picture.id)
      }

    case getPicture(pic_id, token) =>
      val clientID = Keys.tokenMap.getOrElse(token, -1)
      // update the access token time out map
      val nonTimeout =
        (Platform.currentTime - Keys.tokenTimeoutMap.getOrElse(token, 0).asInstanceOf[Long])  < Keys.TIME_OUT
      if (nonTimeout) { Keys.tokenTimeoutMap.put(token, Platform.currentTime) }

      val picture = Picture.picMap.getOrElse(pic_id, null)
      if (picture != null && nonTimeout) {
        val key_map = Util.stringToMap(picture.key_map)
        val encrypted_key = key_map.getOrElse(clientID, null)
        if (encrypted_key != null) {
          picture.encrypted_key_=(encrypted_key)
          sender ! Picture.toJson(picture)
        } else {
          sender ! "Permission Denied"
        }
      } else {
        sender ! "Permission Denied"
      }

    case deletePicture(pic_id, token) =>
      val clientID = Keys.tokenMap.getOrElse(token, -1)
      if (Picture.picMap.getOrElse(pic_id, null) != null
        && clientID == Picture.picMap.getOrElse(pic_id, null).creator_id) {

        val picture = Picture.picMap.get(pic_id).orNull
        Picture.picMap.remove(pic_id)

        if (picture != null && picture.album_id != -1) {
          val album = Album.albumMap.get(picture.album_id).orNull
          if (album != null) {
            Album.albumMap.get(picture.album_id).orNull.pic_id_list_= (album.pic_id_list.filter(_!= pic_id))
          }
        }
        sender ! "{\"success\" : \"true\"}"
      } else {
        sender ! "{\"success\" : \"false\"}"
      }

  }
}


