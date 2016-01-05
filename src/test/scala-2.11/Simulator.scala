import java.security.{KeyPair, KeyPairGenerator, SecureRandom}
import javax.crypto.{Cipher, KeyGenerator, SealedObject, SecretKey}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.util.Timeout
import org.json4s._
import org.json4s.native.JsonMethods._
import spray.client.pipelining._
import spray.http._
import sun.misc.BASE64Encoder

import scala.collection.mutable.ListBuffer
import scala.compat.Platform
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

/**
 * Created by jiaxing song on 2015/11/27.
 * Create virtual client with random behaviors
 *
 */
object Simulator {
  case class register(name : String)

  implicit val system = ActorSystem("simulator")
  import system.dispatcher
  val pipeline = sendReceive
  val numClient = 200
  val random = Random
  var start : Long = 0
  var numRequest = 0
  var postList  = new ListBuffer[Int]()
  var pictureList  = new ListBuffer[Int]()
  var albumList = new ListBuffer[Int]()
  val maxNumFriends = 40
  postList += 0
  pictureList += 0
  albumList += -1


  def main (args: Array[String]): Unit = {
    val system = ActorSystem("simulator")
    val master = system.actorOf(Props(new Master()), name = "master")
    val platform = Platform
    start = platform.currentTime
    master ! "register"
  }

  class Master extends Actor {

    var numResponse = 0
    var clientList = new ListBuffer[ActorRef]()
    for (i <- 0 until numClient) {
      clientList += context.actorOf(Props(new Client()), name = "client"+i)
    }

    def receive = {
      case "register" =>
        println("Registering and creating the pages...")
        for (i <- 0 until numClient) {
          clientList(i) ! register("client"+i)
        }
      case "page created" =>
        numResponse += 1
        if (numResponse == numClient) {
          numResponse = 0
          self ! "init"
        }
      case "init" =>
        println("Initialization...")
        for (i <- 0 until numClient) {
          clientList(i) ! "init"
        }
      case "friends added" =>
        numResponse += 1
        if (numResponse >= 0.7 * numClient) {
          numResponse = 0
          self ! "log in"
        }
      case "log in" =>
        println("Users log in...")
        for (i <- 0 until numClient) {
          clientList(i) ! "login"
        }
      case "logged in" =>
        numResponse += 1
        if (numResponse >= numClient) {
          numResponse = 0
          println("Start random behavior...")
          for (i <- 0 until numClient) {
            clientList(i) ! "start"
          }
        }
      case "done" =>
        numResponse += 1
        if (numResponse == numClient) {
          numResponse = 0
          println("All clients are done!")
          println("Total number of requests:" + numRequest)
          println("running time: "+ (System.currentTimeMillis() - start)/1000+"s")
          System.exit(0)
        }
    }
  }

  class Client extends Actor {

    var page_name: String = null
    var page_id: Int = -1
    var album_id: Int = -1
    var friend_id_list = List[Int]()
    var key_list = List[String]()
    var client_post_list = new ListBuffer[Int]()
    var client_pic_list = new ListBuffer[Int]()
    var numResponse = 0
    // RSA encryption
    private val keyGen = KeyPairGenerator.getInstance("RSA")
    keyGen.initialize(2048, new SecureRandom())
    private val keyPair: KeyPair = keyGen.generateKeyPair()
    private val rsaCipher = Cipher.getInstance("RSA")
    private var accessToken: String = null
    // AES
    private val symmetricKeyGen = KeyGenerator.getInstance("AES")
    symmetricKeyGen.init(128, new SecureRandom())

    def receive = {
      case register(name: String) =>
        val publicKeyStr = new BASE64Encoder().encode(keyPair.getPublic.getEncoded)
        numRequest += 1
        if (numRequest % Math.max(numClient / 100, 1) == 0) {
          println("registration and creating pages status:"+ (numRequest * 100.0 / numClient)+"%")
        }
        val uri = Uri("http://localhost:8080/page")
        // send public key to the server
        val data = Map("name" -> name, "public_key" -> publicKeyStr)
        val post = Post(uri, FormData(data))
        page_name = name
        val responseString = sendRequest(post).entity.asString
        page_id = responseString.toInt
        context.parent ! "page created"

      case "init" =>
        // 80% percent of friend requests will be accepted
        val numFriends = random.nextInt(maxNumFriends) + 1

        for (i <- 1 to numFriends) {
          val friendId = random.nextInt(numClient)
          if (random.nextInt(100) < 80) {
            val uri = Uri("http://localhost:8080/friend?id1=" + page_id + "&id2=" + friendId)
            val post = Post(uri)
            numRequest += 1
            for (response <- pipeline(post).mapTo[HttpResponse]) yield {
              numResponse += 1
              if (numResponse >= numFriends) {
                context.parent ! "friends added"
              }
            }
          } else {
            numResponse += 1
            if (numResponse >= numFriends) {
              context.parent ! "friends added"
            }
          }
        }
        if (numFriends == 0) {
          context.parent ! "friends added"
        }

      case "login" =>
        try {
          // send request to login and get the challenge from server
          val get = Get("http://localhost:8080/login/" + page_id)
          val randomStr = (parse(sendRequest(get).entity.asString) \ "random_num").values.asInstanceOf[String]

          // create a summary of the large random number
          val sha256RandomStr = Util.sha256(randomStr.getBytes)

          // create digit signature and encode it to string
          rsaCipher.init(Cipher.ENCRYPT_MODE, keyPair.getPrivate)
          val digitSigStr = Util.sealedObjToString(new SealedObject(sha256RandomStr, rsaCipher))

          // respond with digit signature
          val data = Map("id" -> page_id.toString, "digitSig" -> digitSigStr)
          val post = Post(Uri("http://localhost:8080/login"), FormData(data))
          val response = sendRequest(post)
          val status = (parse(response.entity.asString) \ "success").values.asInstanceOf[String].toBoolean

          // if login succeed, initialize the client
          if (status) {
            init(response)
            context.parent ! "logged in"
          } else { println("Login Failure") }
        }
        catch {
          case parseException: Exception => println("Login Error")
        }
      case "start" =>
        // every user will set the profile first
        try {
          (parse(sendRequest(buildSecureProfileRequest(friend_id_list, key_list)).entity.asString)
            \ "success").values.asInstanceOf[String]
          numRequest += 1
        } catch {
          case parseException: Exception => println("set profile failed")
        }
        //passive user
        val randomNum = random.nextInt(100)
        if (randomNum < 75) {
          // 27% get profiles
          var get = Get()
          if (randomNum < 21) {
            get = Get(Uri("http://localhost:8080/profile/" + page_id+"?access_token="+accessToken))
            decryptProfile(sendRequest(get).entity.asString)
            numRequest += 1
          }
          // 46% get posts
          else if (randomNum >= 21 && randomNum < 56) {
            if (friend_id_list.nonEmpty) {
              for (i <- friend_id_list.indices) {
                get = Get(Uri("http://localhost:8080/page/" + friend_id_list(i) + "?access_token=" + accessToken))
                val friend_postId_list = (parse(sendRequest(get).entity.asString) \ "_postIdList").values.asInstanceOf[List[Int]]
                numRequest += 1
                for (j <- friend_postId_list.indices) {
                  get = Get(Uri("http://localhost:8080/post/" + friend_postId_list(j) + "?access_token=" + accessToken))
                  val responseData = sendRequest(get).entity.asString
                  decryptPost(responseData)
                  numRequest += 1
                }
              }
            } else {
              get = Get(Uri("http://localhost:8080/post/" + random.nextInt(postList.size) + "?access_token=" + accessToken))
              decryptPost(sendRequest(get).entity.asString)
            }
          }
          // 27% get picture and album
          else {
            get = Get(Uri("http://localhost:8080/album/" + random.nextInt(albumList.size)+"?access_token="+accessToken))
            get = Get(Uri("http://localhost:8080/picture/" + random.nextInt(pictureList.size)+"?access_token="+accessToken))
            decryptPicture(sendRequest(get).entity.asString)
            numRequest += 1
          }
          context.parent ! "done"
        }
        // active user
        else {
          // set profiles
          var post = Post()
          if (randomNum >= 75 && randomNum < 79) {
            addFriend(random.nextInt(numClient))
          }
          if (randomNum >= 79 && randomNum < 82) {
            post = buildSecureProfileRequest(friend_id_list, key_list)
            try {
              (parse(sendRequest(post).entity.asString) \ "success").values.asInstanceOf[String]
              println("Post Request Success")
            } catch {
              case parseException: Exception => println("set profile failed")
            }
            numRequest += 1
          }
          // create new posts
          else if (randomNum >= 82 && randomNum < 92) {
            for (i <- 1 to 10) {
              post = buildSecurePostRequest(friend_id_list, key_list)
              try {
                val post_id = (parse(sendRequest(post).entity.asString) \ "post_id").values.asInstanceOf[String]
                postList += post_id.toInt
                client_post_list += post_id.toInt
                println("Post Request Success")
              } catch {
                case parseException: Exception => println("create new post failed")
              }
            }
            numRequest += 10
          }
          // create pictures and albums
          else {
            for (i <- 1 to 10) {
              post = buildSecurePictureRequest(friend_id_list, key_list)
              val album_post = buildSecureAlbumRequest(friend_id_list, key_list)
              try {
                val picture_id = (parse(sendRequest(post).entity.asString) \ "picture_id").values.asInstanceOf[String]
                pictureList += picture_id.toInt
                client_pic_list += picture_id.toInt
                println("Post Request Success")

                val album_id = (parse(sendRequest(album_post).entity.asString) \ "album_id").values.asInstanceOf[String]
                albumList += album_id.toInt
                println("Post Request Success")
              } catch {
                case parseException: Exception => println("upload picture or album failed")
              }
            }
            numRequest += 10
          }
          context.parent ! "done"
        }

      case "test" =>
        test
    }

    private def sendRequest(request: HttpRequest): HttpResponse = {
      val result = Await.result(pipeline(request), Timeout(100 seconds).duration)
      result
    }

    /**
     * After successfully login, the client start initialization.
     * 1) Request the friend list,
     * 2) Get the public key of each friend
     * 3) Post the encrypted public keys to the server
     * */
    private def init(httpResponse: HttpResponse) = {
      try {

        // get the access token
        accessToken = (parse(httpResponse.entity.asString) \ "access_token").values.asInstanceOf[String]
        println("access token",accessToken,page_id)
        val get = Get("http://localhost:8080/page/" + page_id + "?access_token=" + accessToken)
        val responseData = parse(sendRequest(get).entity.asString)

        // get the friend list
        friend_id_list = (responseData \ "_friendlist" \ "_idList").values.asInstanceOf[List[Int]]

        // get the public key list of each friend
        key_list = (responseData \ "_keyStringList").values.asInstanceOf[List[String]]
      } catch {
        case ex : Exception => println("client initialization error")
      }
    }

    private def buildSecureProfileRequest(friend_id_list : List[Int], key_list : List[String]): HttpRequest = {
      // generate a new symmetric key and encrypt the key with different public key and post to server
      symmetricKeyGen.init(128, new SecureRandom())
      val symmetricKey = symmetricKeyGen.generateKey()
      var keyMap : Map[Int, String] = Map()
      rsaCipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic)
      keyMap += (page_id -> Util.sealedObjToString(new SealedObject(symmetricKey, rsaCipher)))
      for (i <- friend_id_list.indices) {
        rsaCipher.init(Cipher.ENCRYPT_MODE, Util.stringToPublicKey(key_list(i)))
        keyMap += (friend_id_list(i) -> Util.sealedObjToString(new SealedObject(symmetricKey, rsaCipher)))
      }

      // generate random IV and encrypt the information
      val IV = Util.generateIV()
      val workplace = AES.encrypt(symmetricKey, IV, "work place of " + page_name)
      val school = AES.encrypt(symmetricKey, IV, "school of " + page_name)
      val email = AES.encrypt(symmetricKey, IV, "email of " + page_name)
      val current_place = AES.encrypt(symmetricKey, IV, "current place of " + page_name)
      val data = Map("id" -> page_id.toString,
        "name" -> page_name,
        "workplace" -> workplace,
        "school" -> school,
        "email" -> email,
        "current_place" -> current_place,
        "iv" -> IV,
        "key_map" -> Util.mapToString(keyMap),
        "access_token" -> accessToken)
      Post(Uri("http://localhost:8080/profile"), FormData(data))
    }

    private def buildSecurePostRequest(friend_id_list : List[Int], key_list : List[String]): HttpRequest = {
      symmetricKeyGen.init(128, new SecureRandom())
      val symmetricKey = symmetricKeyGen.generateKey()
      var keyMap : Map[Int, String] = Map()
      rsaCipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic)
      keyMap += (page_id -> Util.sealedObjToString(new SealedObject(symmetricKey, rsaCipher)))
      for (i <- friend_id_list.indices) {
        rsaCipher.init(Cipher.ENCRYPT_MODE, Util.stringToPublicKey(key_list(i)))
        keyMap += (friend_id_list(i) -> Util.sealedObjToString(new SealedObject(symmetricKey, rsaCipher)))
      }

      val IV = Util.generateIV()
      val message = AES.encrypt(symmetricKey, IV, "some message of user " + page_name)
      val data = Map("creator_id" -> (page_id + ""),
        "create_time" -> "some time",
        "message" -> message,
        "iv" -> IV,
        "key_map" -> Util.mapToString(keyMap),
        "access_token" -> accessToken)
      Post(Uri("http://localhost:8080/post"), FormData(data))
    }

    private def buildSecurePictureRequest(friend_id_list : List[Int], key_list : List[String]): HttpRequest = {
      symmetricKeyGen.init(128, new SecureRandom())
      val symmetricKey = symmetricKeyGen.generateKey()
      var keyMap : Map[Int, String] = Map()
      rsaCipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic)
      keyMap += (page_id -> Util.sealedObjToString(new SealedObject(symmetricKey, rsaCipher)))
      for (i <- friend_id_list.indices) {
        rsaCipher.init(Cipher.ENCRYPT_MODE, Util.stringToPublicKey(key_list(i)))
        keyMap += (friend_id_list(i) -> Util.sealedObjToString(new SealedObject(symmetricKey, rsaCipher)))
      }

      val IV = Util.generateIV()
      val picture_data = AES.encrypt(symmetricKey, IV, "some picture data if user " + page_name)
      val data = Map("album_id" -> random.nextInt(albumList.length).toString,
        "creator_id" -> page_id.toString,
        "create_time" -> "some time",
        "picture_data" -> picture_data,
        "iv" -> IV,
        "key_map" -> Util.mapToString(keyMap),
        "access_token" -> accessToken)
      Post(Uri("http://localhost:8080/picture"), FormData(data))
    }

    def buildSecureAlbumRequest(friend_id_list : List[Int], key_list : List[String]) : HttpRequest = {
      symmetricKeyGen.init(128, new SecureRandom())
      val symmetricKey = symmetricKeyGen.generateKey()
      var keyMap : Map[Int, String] = Map()
      rsaCipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic)
      keyMap += (page_id -> Util.sealedObjToString(new SealedObject(symmetricKey, rsaCipher)))
      for (i <- friend_id_list.indices) {
        rsaCipher.init(Cipher.ENCRYPT_MODE, Util.stringToPublicKey(key_list(i)))
        keyMap += (friend_id_list(i) -> Util.sealedObjToString(new SealedObject(symmetricKey, rsaCipher)))
      }

      val data = Map("album_id" -> random.nextInt(albumList.length).toString,
        "creator_id" -> page_id.toString,
        "create_time" -> "some time",
        "key_map" -> Util.mapToString(keyMap),
        "access_token" -> accessToken)
      Post(Uri("http://localhost:8080/album"), FormData(data))
    }

    def decryptProfile(profileJson : String) {
      var parsedJson : JValue = null
      try {
        parsedJson = parse(profileJson)
        val pair = getKeyAndIV(parsedJson)
        // use iv and symmetricKey to decrypt the content
        val workplace = AES.decrypt(pair._1, pair._2, (parsedJson \ "_workplace").values.asInstanceOf[String])
        val school = AES.decrypt(pair._1, pair._2, (parsedJson \ "_school").values.asInstanceOf[String])
        val email = AES.decrypt(pair._1, pair._2, (parsedJson \ "_email").values.asInstanceOf[String])
        val current_place = AES.decrypt(pair._1, pair._2, (parsedJson \ "_current_place").values.asInstanceOf[String])
        println(page_id, "Decode profile succeed.",workplace, school, email, current_place)
      } catch {
        case ex : Exception => println(page_id, "Profile Permission Denied.")
      }
    }

    private def decryptPost(postJson : String) {
      var parsedJson : JValue = null
      try {
        parsedJson = parse(postJson)
        val pair = getKeyAndIV(parsedJson)
        // use iv and symmetricKey to decrypt the content
        val message = AES.decrypt(pair._1, pair._2, (parsedJson \ "_message").values.asInstanceOf[String])
        println(page_id, "Decode post succeed.",message)
      } catch {
        case ex : Exception => println(page_id, "Post Permission Denied.")
      }
    }

    private def decryptPicture(picJson : String) {
      var parsedJson : JValue = null
      try {
        parsedJson = parse(picJson)
        val pair = getKeyAndIV(parsedJson)
        // use iv and symmetricKey to decrypt the content
        val picture_data = AES.decrypt(pair._1, pair._2, (parsedJson \ "_picture_data").values.asInstanceOf[String])
        println(page_id, "Decode picture succeed.", picture_data)
      } catch {
        case ex : Exception => println(page_id, "Picture Permission Denied.")
      }
    }

    private def getKeyAndIV(parsedJson : JValue) : (SecretKey, String) = {
      // decrypt the RSA to get symmetric key
      val encrypted_key = (parsedJson \ "_encrypted_key").values.asInstanceOf[String]
      rsaCipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate)
      val symmetricKey = Util.decodeSealedObject(encrypted_key).getObject(rsaCipher).asInstanceOf[SecretKey]
      // get IV
      val iv = (parsedJson \ "_iv").values.asInstanceOf[String]
      (symmetricKey, iv)
    }

    private def addFriend(friend_id : Int) = {
      val symmetricKey = symmetricKeyGen.generateKey()
      val get = Get(Uri("http://localhost:8080/public_key/"+friend_id))
      val publicKeyStr = (parse(sendRequest(get).entity.asString) \ "public_key").values.asInstanceOf[String]
      rsaCipher.init(Cipher.ENCRYPT_MODE, Util.stringToPublicKey(publicKeyStr))
      val encrypted_key = Util.sealedObjToString(new SealedObject(symmetricKey, rsaCipher))
      val data = Map("target_id" -> (friend_id + ""),
        "encrypted_key" -> encrypted_key,
        "access_token" -> accessToken)
      val post = Post(Uri("http://localhost:8080/add_connection"), FormData(data))
      try {
        println("Add connection:"+sendRequest(post).entity.asString)
      } catch {
        case ex : Exception => println("Failed to add new connection.")
      }
    }

    private def test() = {
      println("start testing...")

      var post = buildSecureProfileRequest(friend_id_list, key_list)
      var status = (parse(sendRequest(post).entity.asString) \ "success").values.asInstanceOf[String]

      post = buildSecurePostRequest(friend_id_list, key_list)
      status = (parse(sendRequest(post).entity.asString) \ "post_id").values.asInstanceOf[String]
      postList += status.toInt

      post = buildSecurePictureRequest(friend_id_list, key_list)
      status = (parse(sendRequest(post).entity.asString) \ "picture_id").values.asInstanceOf[String]
      pictureList +=status.toInt

      // get all the profile from friends
      for (i <- friend_id_list.indices) {
        println(page_id,page_name, "GET" ,friend_id_list(i), friend_id_list)
        val getProfile = Get(Uri("http://localhost:8080/profile/" + friend_id_list(i)+"?access_token="+accessToken))
        val response = sendRequest(getProfile).entity.asString
        decryptProfile(response)
      }
      // get all the post from friends
      for (i <- postList.indices) {
        val getPost = Get(Uri("http://localhost:8080/post/" + i+"?access_token="+accessToken))
        val response = sendRequest(getPost).entity.asString
        decryptPost(response)
      }
      // get all the pictures from friends
      for (i <- pictureList.indices) {
        val getPicture = Get(Uri("http://localhost:8080/picture/" + i+"?access_token="+accessToken))
        val response = sendRequest(getPicture).entity.asString
        decryptPicture(response)
      }
    }
  }
}


