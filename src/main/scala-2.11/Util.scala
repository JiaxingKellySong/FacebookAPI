import java.io.{ByteArrayInputStream, ObjectInputStream, ObjectOutputStream}
import java.math.BigInteger
import java.security.spec.X509EncodedKeySpec
import java.security.{KeyFactory, PublicKey, SecureRandom}
import java.util.Base64
import javax.crypto.{KeyGenerator, SealedObject, SecretKey}

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream
import sun.misc.{BASE64Decoder, BASE64Encoder}

/**
 * Created by jiaxing song on 2015/12/10.
 * Provide some basic functions for data manipulation
 */
object Util {
  val secureRandom = new SecureRandom()

  def stringToPublicKey(keyStr : String) : PublicKey = {
    if (keyStr == null) { return null }
    val X509publicKey = new X509EncodedKeySpec(new BASE64Decoder().decodeBuffer(keyStr))
    val kf = KeyFactory.getInstance("RSA")
    kf.generatePublic(X509publicKey)
  }

  def publicKeyToString(key : PublicKey) : String = {
    if (key == null) { return null}
    new BASE64Encoder().encode(key.getEncoded)
  }

  def decodeSealedObject(objStr : String) : SealedObject = {
    if (objStr == null) { return null }
    val objIn = new ObjectInputStream(new ByteArrayInputStream(Base64.getDecoder.decode(objStr)))
    objIn.readObject().asInstanceOf[SealedObject]
  }

  def sha256(bytes : Array[Byte]) : String = {
    java.security.MessageDigest.getInstance("SHA-256").digest(bytes).mkString
  }

  def sealedObjToString(obj : SealedObject) : String = {
    if (obj == null) { return null }
    val byteStream = new ByteOutputStream()
    val objOut = new ObjectOutputStream(byteStream)
    objOut.writeObject(obj)
    Base64.getEncoder.encodeToString(byteStream.getBytes)
  }

  def byteArrayToString(bytes : Array[Byte]) : String = {
    Base64.getEncoder.encodeToString(bytes)
  }

  def generateAccessToken() : String = {
    new BigInteger(512, secureRandom).toString(32)
  }

  def stringToMap(str : String) :  Map[Int, String] = {
    val objIn = new ObjectInputStream(new ByteArrayInputStream(Base64.getDecoder.decode(str)))
    objIn.readObject().asInstanceOf[Map[Int, String]]
  }

  def mapToString(map : Map[Int, String]) : String = {
    val byteStream = new ByteOutputStream()
    val objOut = new ObjectOutputStream(byteStream)
    objOut.writeObject(map)
    Base64.getEncoder.encodeToString(byteStream.getBytes)
  }


  def generateAESKey() : SecretKey = {
    val keyGen = KeyGenerator.getInstance("AES")
    keyGen.init(128)
    keyGen.generateKey()
  }

  def generateIV() : String = {
    val bytes = new Array[Byte](16)
    secureRandom.nextBytes(bytes)
    Base64.getEncoder.encodeToString(bytes)
  }
}
