import java.io.{ByteArrayInputStream, ObjectInputStream, ObjectOutputStream}
import java.security.{KeyPair, KeyPairGenerator, SecureRandom}
import javax.crypto.{Cipher, KeyGenerator, SealedObject}

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream
import node.Profile
import sun.misc.{BASE64Decoder, BASE64Encoder}
/**
 * Created by jiaxing song on 2015/11/20.
 */
object Test extends App{
  def printProfile (profile : Profile) = {
    println(profile.name+" "+profile.workplace+" "+profile.school+" "+profile.email+" "+profile.current_place)
  }

  val keyGen = KeyPairGenerator.getInstance("RSA")
  keyGen.initialize(1024, new SecureRandom())
  val keyPair : KeyPair = keyGen.generateKeyPair()
  val publicKey = keyPair.getPublic
  val privateKey = keyPair.getPrivate
  val message = "test"
//
  val encoder = new BASE64Encoder()
////  val publicKeyStr = encoder.encode(publicKey.getEncoded)
//  val publicKeyStr = new BASE64Encoder().encode(publicKey.getEncoded)
//  println(publicKey.toString)
//
  val decoder = new BASE64Decoder()
//
//  val bytes = new BASE64Decoder().decodeBuffer(publicKeyStr)
//  val X509publicKey = new X509EncodedKeySpec(bytes);
//  val kf = KeyFactory.getInstance("RSA")
//  val recoveredPublicKey = kf.generatePublic(X509publicKey)
//  println(recoveredPublicKey)
//  assert(publicKey.equals(recoveredPublicKey))

  val cipher = Cipher.getInstance("RSA")
  cipher.init(Cipher.ENCRYPT_MODE, privateKey)
  val encryptedMessage = new SealedObject(message, cipher)


  val decipher = Cipher.getInstance("RSA")
  decipher.init(Cipher.DECRYPT_MODE, privateKey)

//  println("decipher: "+ encryptedMessage.getObject(decipher))
//  private implicit val formats = DefaultFormats + FieldSerializer[SealedObject]()

//  val encryptedJson = writePretty(encryptedMessage)
//  println("json: "+ encryptedJson)
//
//  val parsedJson = parse(encryptedJson)
//  println(parsedJson.values)

//
//

  val byteStream = new ByteOutputStream()
  val objOut = new ObjectOutputStream(byteStream)
  objOut.writeObject(encryptedMessage)

  val byteStr = encoder.encode(byteStream.getBytes)
  val objIn = new ObjectInputStream(new ByteArrayInputStream(decoder.decodeBuffer(byteStr)))


//  println(assert(objIn.readObject().asInstanceOf[SealedObject].equals(encryptedMessage)))
//    println(objIn.readObject().asInstanceOf[SealedObject].getObject(decipher))

//  AES.testFunction("test function in java")
//  val key: String = "Bar12345Bar12345"
//  val initVector: String = "RandomInitVector"
//  println(AES.decrypt(key, initVector,AES.encrypt(key, initVector, "Hello World")))

//  val byte = new Array[Byte](64)
//  val secureRandomGenerator = new SecureRandom()
//  secureRandomGenerator.nextBytes(byte)
//  val accessToken = Base64.getEncoder.encodeToString(byte)
//  println( "{\"success\" : \"true\", \"access_token\" : \""+accessToken+"\"}")

//  println(Util.generateAccessToken())
//  println(Util.generateAccessToken().length)

//  val map = new ConcurrentHashMap[Int, String]()
//  map.put(1,"1")
//  map.put(2,"2")

//  println(map.toString)
//  println(Util.mapToString(map))
//  println(Util.stringToMap(Util.mapToString(map)))

//  println(Util.generateAESKey())
//  println(Util.generateAESKey())
//  println(Util.generateAESKey())

//  var maps : Map[Int, Map[Int, String]] = Map()
  //  var map11 : Map[Int, String] = Map()
  //  var map12 : Map[Int, String] = Map()
  //  var map21 : Map[Int, String] = Map()
  //  var map22 : Map[Int, String] = Map()
  //
  //  val k1 = 1
  //  val v1 = "1"
  //  val k2 = 2
  //  val v2 = "2"
  //  map11 += (k1 -> v1)
  //  map11 += (k2 -> v2)
  //  map21 += (k1 -> v1)
  //  map21 += (k2 -> v2)
  //
  //  maps += (1 -> map11)
  //  maps += (2 -> map21)
  //
  //  println(maps)
  //  println(1,maps.getOrElse(1, null).getOrElse(1, null))
  //  println(1,maps.getOrElse(1, null).getOrElse(2, null))
  //  println(2,maps.getOrElse(2, null).getOrElse(1, null))
  //  println(2,maps.getOrElse(2, null).getOrElse(2, null))

  for (i <- 1 to 10000) {
    println("once")
    val keyGen = KeyPairGenerator.getInstance("RSA")
    keyGen.initialize(1024, new SecureRandom())
    val keyPair: KeyPair = keyGen.generateKeyPair()
    val rsaCipher = Cipher.getInstance("RSA")
    var accessToken: String = null
    // AES
    val symmetricKeyGen = KeyGenerator.getInstance("AES")
    symmetricKeyGen.init(128)
    var symmetricKey = symmetricKeyGen.generateKey()
  }
}
