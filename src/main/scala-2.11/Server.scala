import akka.actor.{ActorSystem, Props}
import akka.pattern._
import akka.routing.RoundRobinRouter
import akka.util.Timeout
import data.{Album, Keys, Picture, Post}
import node.{Page, Album, Picture, Post}
import spray.http.MediaTypes
import spray.routing.{Route, SimpleRoutingApp}

import scala.compat.Platform
import scala.concurrent.duration._
import scala.util.Success

object Server extends App with SimpleRoutingApp {

  val numPageCreator = 4
  val numPageGetter = 16
  val numProfileSetter = 4
  val numProfileGetter = 16
  val numFriendManager = 4
  val numPostManager = 16
  val numPictureManager = 8
  val numAlbumManager = 8
  val numAuthManager = 8

  implicit val actorSystem = ActorSystem()
  implicit val timeout = Timeout(100 seconds)

  val pageManagerRouter = actorSystem.actorOf(Props[PageManager].withRouter(
    RoundRobinRouter(nrOfInstances = numPageCreator)))
  val profileManagerRouter = actorSystem.actorOf(Props[ProfileManager].withRouter(
    RoundRobinRouter(nrOfInstances = numProfileSetter)))
  val friendManagerRouter = actorSystem.actorOf(Props[FriendListManager].withRouter(
    RoundRobinRouter(nrOfInstances = numFriendManager)))
  val postManagerRouter = actorSystem.actorOf(Props[PostManager].withRouter(
    RoundRobinRouter(nrOfInstances = numPostManager)))
  val pictureManagerRouter = actorSystem.actorOf(Props[PictureManager].withRouter(
    RoundRobinRouter(nrOfInstances = numPictureManager)))
  val albumManagerRouter = actorSystem.actorOf(Props[AlbumManager].withRouter(
    RoundRobinRouter(nrOfInstances = numAlbumManager)))
  val authenticationManagerRouter = actorSystem.actorOf(Props[AuthenticationManager].withRouter(
    RoundRobinRouter(nrOfInstances = numAuthManager)))
  
  trait ServerMessage
  // page
  case class createPage(page_id : Int, name : String, key : String)
  case class getPage(page_id : Int, access_token : String)
  case class deletePage(page_id : Int, access_token : String)
  // profile
  case class getProfile(id : Int, token : String)
  case class setProfile(id : Int,
                         name : String,
                         workplace : String,
                         school : String,
                         email : String,
                         current_place : String,
                         iv : String,
                         keyMap : String,
                         token : String)
  // friend list
  case class addFriend(id1 : Int, id2 : Int)
  case class deleteFriend(id1 :Int, id2 : Int)
  // post
  case class createPost( post_id : Int,
                         creator_id : Int,
                         create_time : String,
                         message : String,
                         iv : String,
                         token : String)
  case class getPost(post_id : Int, token : String)
  case class deletePost(post_id : Int, token : String)
  // picture
  case class createPicture(pic_id : Int,
                           album_id : Int,
                           creator_id : Int,
                           create_time : String,
                           picture_data : String,
                           iv : String,
                           token : String)
  case class getPicture(picture_id : Int, token : String)
  case class deletePicture(picture_id : Int, token : String)
  // album
  case class createAlbum (album_id : Int,
                           creator_id : Int,
                           create_time : String,
                          token : String)
  case class getAlbum(album_id : Int, token : String)
  case class deleteAlbum(album_id : Int, token : String)
  //authentication
  case class loginRequest(id : Int)
  case class loginDigitSignature(id : Int, digitSig : String)

  def getJson(route: Route) = get {
    respondWithMediaType(MediaTypes.`application/json`) { route }
  }
  implicit def executionContext = actorRefFactory.dispatcher

  startServer(interface = "localhost", port = 8080) {
    get {
      path("home") { ctx =>
        ctx.complete("Welcome to facebook simulator server!")
      }
    } ~
    get {
      path("login" / IntNumber) { page_id =>
        val status = ask(authenticationManagerRouter, loginRequest(page_id)).mapTo[String]
        onComplete(status) {
          case Success(randStr) => complete {
            "{\"random_num\" : \""+randStr+"\"}"
          }
        }
      }
    } ~
    post {
      path("login") {
        formFields('id.as[Int], 'digitSig.as[String]) {
          (id, digitSig) => {
            val status = ask(authenticationManagerRouter, loginDigitSignature(id, digitSig)).mapTo[String]
            onComplete(status) {
              case Success(status) =>
                complete {
                  status
                }
            }
          }
        }
      }
    } ~
    post {
      path("page")
      formFields('name.as[String], 'public_key.as[String]){
        (name, keyStr) =>
          Page.id += 1
          complete {
            Page.pagesMap.put(Page.id, new Page(Page.id, name))
            val publicKey = Util.stringToPublicKey(keyStr)
            Keys.keyMap.put(Page.id, publicKey)
            Page.id + ""
          }
        }
    } ~
    getJson {
      path("page" / IntNumber) {
        id =>
        parameter("access_token") {
          token => {
            val pageStr = ask(pageManagerRouter, getPage(id, token)).mapTo[String]
            onComplete(pageStr) {
              case Success(pageStr) =>
                complete {
                  pageStr
                }
            }
          }
        }
      }
    } ~
    delete {
      path ("page" / IntNumber) {
        id =>
        parameter("access_token") {
          token =>
            pageManagerRouter ! deletePage(id, token)
            complete {
              "{ \"success\" : \"true\" }"
            }
        }
      }
    } ~
    post {
      path("profile") {
        formFields('id.as[Int],
          'name.as[String],
          'workplace.as[String],
          'school.as[String],
          'email.as[String],
          'current_place.as[String],
          'iv.as[String],
          'key_map.as[String],
          'access_token.as[String]) { (id, name, workplace, school, email, current_place, iv, key_map, token) =>
          val status = ask(profileManagerRouter,
            setProfile(id, name, workplace, school, email, current_place, iv, key_map, token)).mapTo[String]
          onComplete(status) {
            case Success(status) =>
              complete {
                "{\"success\" :"+"\""+status+"\"}"
              }
          }
        }
      }
    } ~
    getJson {
      path("profile" / IntNumber) {
        id =>
        parameter("access_token") {
          token =>
            val jsonStr = ask(profileManagerRouter, getProfile(id, token)).mapTo[String]
            onComplete(jsonStr) {
              case Success(jsonStr) =>
                complete{
                  jsonStr
                }
            }
        }
      }
    } ~
    post {
      path("friend") {
        parameter("id1", "id2") {
          (pid1, pid2) => {
            complete {
              val id1 = pid1.toInt
              val id2 = pid2.toInt
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

                "{\"success\" : \"true\"}"
              } else {
                "{\"success\" : \"false\"}"
              }
            }

          }
        }
      }
    } ~
    delete {
      path("friend") {
        parameter("id1", "id2") {
          (id1, id2) => {
            friendManagerRouter ! deleteFriend(id1.toInt, id2.toInt)
            complete {
              "{ success : true }"
            }
          }
        }
      }
    } ~
    post {
      path("post") {
        formFields('creator_id.as[Int],
          'create_time.as[String],
          'message.as[String],
          'iv.as[String],
          'key_map.as[String],
          'access_token.as[String]) { (creator_id, create_time, message, iv, key_map, token) =>
          complete {
            val clientID = Keys.tokenMap.getOrElse(token, -1)
            val nonTimeout =
              (Platform.currentTime - Keys.tokenTimeoutMap.getOrElse(token, 0).asInstanceOf[Long])  < Keys.TIME_OUT
            if (nonTimeout) { Keys.tokenTimeoutMap.put(token, Platform.currentTime) }

            val page = Page.pagesMap.getOrElse(creator_id, null)
            if (clientID != creator_id && page != null && nonTimeout) {
              "{\"post_id\" : \"-1\"}"
            } else {
              Post.id += 1
              var new_post = new Post(Post.id)
              new_post.creator_id_=(creator_id)
              new_post.create_time_=(create_time)
              new_post.message_=(message)
              new_post.iv_=(iv)
              new_post.key_map_=(key_map)
              Post.postMap.put(Post.id, new_post)
              page.postIdList_=(page.postIdList.::(Post.id))
              "{ \"post_id\" : \""+Post.id+"\" }"
            }
          }
        }
      }
    } ~
    getJson {
      path("post" / IntNumber) {
        post_id =>
        parameter("access_token") {
          token =>
            val postStr = ask(postManagerRouter, getPost(post_id, token)).mapTo[String]
            onComplete(postStr) {
              case Success(postStr) =>
                complete {
                  postStr
                }
            }
        }
      }
    } ~
    delete {
      path("post" / IntNumber) {
        post_id =>
        parameter ("access_token") {
          token =>
            val status = ask(postManagerRouter, deletePost(post_id, token)).mapTo[String]
            onComplete(status) {
              case Success(status) =>
                complete {
                  status
                }
            }
        }
      }
    } ~
    post {
      path("picture") {
        formFields( 'album_id.as[Int],
          'creator_id.as[Int],
          'create_time.as[String],
          'picture_data.as[String],
          'iv.as[String],
          'key_map.as[String],
          'access_token.as[String]) { (album_id, creator_id, create_time, picture_data, iv, key_map, token) =>

          complete {
            val clientID = Keys.tokenMap.getOrElse(token, -1)
            val nonTimeout =
              (Platform.currentTime - Keys.tokenTimeoutMap.getOrElse(token, 0).asInstanceOf[Long])  < Keys.TIME_OUT
            if (nonTimeout) { Keys.tokenTimeoutMap.put(token, Platform.currentTime) }

            if (clientID != creator_id || !nonTimeout) {
              "{\"picture_id\" : \"-1\"}"
            } else {
              Picture.id += 1
              var picture = new Picture(Picture.id)
              picture.album_id_=(album_id)
              picture.creator_id_=(creator_id)
              picture.create_time_=(create_time)
              picture.picture_data_=(picture_data)
              picture.key_map_=(key_map)
              picture.iv_=(iv)
              Picture.picMap.put(Picture.id, picture)

              if (album_id != -1
                && Album.albumMap.contains(album_id)
                && Album.albumMap.get(album_id).orNull != null) {
                Album.albumMap.get(album_id).orNull.pic_id_list
                  = Album.albumMap.get(album_id).orNull.pic_id_list.::(Picture.id)
              }
              "{\"picture_id\" : \""+Picture.id +"\"}"
            }
          }
        }
      }
    } ~
    getJson {
      path("picture" / IntNumber) {
        pic_id =>
        parameter("access_token") {
          token =>
            val pictureStr = ask(pictureManagerRouter, getPicture(pic_id, token)).mapTo[String]
            onComplete(pictureStr) {
              case Success(pictureStr) =>
              complete {
                pictureStr
              }
            }
        }
      }
    } ~
    delete {
      path("picture" / IntNumber) {
        pic_id =>
        parameter ("access_token") {
          token => val status = ask(pictureManagerRouter, deletePicture(pic_id, token)).mapTo[String]
            onComplete(status) {
              case Success(status) =>
                complete {
                  status
                }
            }
        }
      }
    } ~
    post {
      path("album") {
        formFields('creator_id.as[Int],
          'create_time.as[String],
          'key_map.as[String],
          'access_token.as[String]) { (creator_id, create_time, key_map, token) =>
          complete {
            val clientID = Keys.tokenMap.getOrElse(token, -1)
            val nonTimeout =
              (Platform.currentTime - Keys.tokenTimeoutMap.getOrElse(token, 0).asInstanceOf[Long])  < Keys.TIME_OUT
            if (nonTimeout) { Keys.tokenTimeoutMap.put(token, Platform.currentTime) }

            if (clientID != creator_id || !nonTimeout) {
              "{\"album_id\" : \"-1\"}"
            } else {
              Album.id += 1
              var album = new Album(Album.id)
              album.creator_id_=(creator_id)
              album.create_time_=(create_time)
              album.key_map_=(key_map)
              Album.albumMap.put(Album.id, album)
              "{\"album_id\" : \""+Album.id +"\"}"
            }
          }
        }
      }
    } ~
    getJson {
      path("album" / IntNumber) {
        album_id =>
        parameter ("access_token") {
          token => val albumStr = ask(albumManagerRouter, getAlbum(album_id, token)).mapTo[String]
            onComplete(albumStr) {
              case Success(albumStr) =>
                complete {
                  albumStr
                }
            }
        }
      }
    } ~
    delete {
      path("album" / IntNumber) {
        album_id =>
        parameter ("access_token") {
          token => val status = ask(albumManagerRouter, deleteAlbum(album_id, token)).mapTo[String]
            onComplete(status) {
              case Success(status) =>
                complete {
                  status
                }
            }
        }
      }
    } ~
    post {
      path("keylist") {
        formFields('id.as[Int], 'access_token.as[String],'keyMap.as[String]) {
          (clientID, accessToken, keyMapStr) =>
          complete {
            val tokenID = Keys.tokenMap.getOrElse(accessToken, -1)
            if (tokenID == clientID) {
              Keys.friendPublicKeyMap += (clientID -> Util.stringToMap(keyMapStr))
              "{ \"success\" : \"true\"}"
            } else {
              "{ \"success\" : \"false\"}"
            }
          }
        }
      }
    } ~
    get {
      path("public_key" / IntNumber) {
        id =>
          complete {
            val publicKey = Keys.keyMap.getOrElse(id, null)
            "{\"public_key\": \""+Util.publicKeyToString(publicKey)+"\"}"
          }
      }
    } ~
    post {
      path("add_connection") {
        formFields('target_id.as[Int],
          'encrypted_key.as[String],
          'access_token.as[String]) {
          (target_id, encrypted_key, token) =>
            complete {
              val id1 = Keys.tokenMap.getOrElse(token, -1)
              val id2 = target_id
              if (id1 != -1
                &&Page.pagesMap.get(id1).orNull != null
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

                var map = Keys.friendPublicKeyMap.getOrElse(id1, null)
                if (map != null) {
                  map += (id2 -> encrypted_key)
                }

                "{\"success\" : \"true\"}"
              } else {
                "{\"success\" : \"false\"}"
              }
            }
        }
      }
    }
  }
}