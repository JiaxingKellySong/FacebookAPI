import java.security._
import javax.crypto.Cipher

import Server.{loginDigitSignature, loginRequest}
import akka.actor.Actor
import data.Keys

import scala.compat.Platform

/**
 * Created by jiaxing song on 2015/12/8.
 * for authentication
 */
class AuthenticationManager extends Actor{
  private val SUCCESS = true
  private val secureRandomGenerator = new SecureRandom()

  def receive = {

    case loginRequest(id: Int) =>
      val bytes = new Array[Byte](512)
      secureRandomGenerator.nextBytes(bytes)
      val randomStr = Util.byteArrayToString(bytes)
      Keys.loginMap.put(id, Util.sha256(randomStr.getBytes))
      sender ! randomStr

    case loginDigitSignature(id, digitSig) =>
      val cipher = Cipher.getInstance("RSA")
      val key = Keys.keyMap.get(id).orNull
      if (key != null) {
        cipher.init(Cipher.DECRYPT_MODE, key)
        val randomStr = Util.decodeSealedObject(digitSig).getObject(cipher)
        // login success
        if (randomStr.equals(Keys.loginMap.get(id).orNull)) {
          sender ! generateResponse(id, SUCCESS)
        }
        else { sender ! generateResponse(id, !SUCCESS) }
        // remove the entry from map as reach random number could be used once
        Keys.loginMap -= id
      }
      else { sender ! generateResponse(id, !SUCCESS) }
  }

  private def generateResponse( id : Int, status : Boolean) : String  = {
    if (status) {
      val accessToken = Util.generateAccessToken()
      Keys.tokenMap.put(accessToken, id)
      Keys.tokenTimeoutMap.put(accessToken, Platform.currentTime)
      "{\"success\" : \"true\", \"access_token\" : \""+accessToken+"\"}"
    } else {
      "{\"success\" : \"false\", \"access_token\" : \"null\"}"
    }
  }

}
