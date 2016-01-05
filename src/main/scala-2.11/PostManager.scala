import Server.{getPost, createPost, deletePost}
import akka.actor.Actor
import data.{Keys, Post}
import node.Post

import scala.compat.Platform

/**
 * Created by jiaxing song on 2015/11/23.
 */
class PostManager extends Actor {

  def receive = {
    case createPost(post_id, creator_id, create_time, message, iv, token) =>
      val clientID = Keys.tokenMap.getOrElse(token, -1)
      if (clientID != creator_id) {
        sender ! "false"
      } else {
        var new_post = new Post(post_id)
        new_post.creator_id_=(creator_id)
        new_post.create_time_=(create_time)
        new_post.message_=(message)
        new_post.iv_=(iv)
        Post.postMap.put(Post.id, new_post)
      }

    case getPost(post_id, token) =>
      val clientID = Keys.tokenMap.getOrElse(token, -1)
      val nonTimeout =
        (Platform.currentTime - Keys.tokenTimeoutMap.getOrElse(token, 0).asInstanceOf[Long])  < Keys.TIME_OUT
      if (nonTimeout) { Keys.tokenTimeoutMap.put(token, Platform.currentTime) }

      val post = Post.postMap.getOrElse(post_id, null)
      if (post != null) {
        val key_map = Util.stringToMap(post.key_map)
        val encrypted_key = key_map.getOrElse(clientID, null)
        if (encrypted_key != null) {
          post.encrypted_key_=(encrypted_key)
          sender ! Post.toJson(post)
        } else {
          sender ! "Permission Denied"
        }
      } else {
        sender ! "Permission Denied"
      }

    case deletePost(post_id, token) =>
      val clientID = Keys.tokenMap.getOrElse(token, -1)
      if (Post.postMap.getOrElse(post_id, null) != null
          && clientID == Post.postMap.getOrElse(post_id, null).creator_id) {
        Post.postMap.remove(post_id)
        sender ! "{\"success\" : \"true\"}"
      } else {
        sender ! "{\"success\" : \"false\"}"
      }

  }
}
