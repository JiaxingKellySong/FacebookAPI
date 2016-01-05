package data

import java.security.PublicKey
import java.util.concurrent.ConcurrentHashMap

import scala.collection.concurrent
import scala.collection.convert.decorateAsScala._

/**
 * Created by jiaxing song on 2015/12/8.
 * For authentication and access control
 */
object Keys {
  val TIME_OUT = 3600000

  // the public key of each user
  var keyMap : concurrent.Map[Int, PublicKey] = new ConcurrentHashMap[Int, PublicKey]().asScala

  // the expected value for authentication
  var loginMap : concurrent.Map[Int, String] = new ConcurrentHashMap[Int, String]().asScala

  // the mapping between access token and client id
  val tokenMap : concurrent.Map[String, Int] = new ConcurrentHashMap[String, Int]().asScala

  // token timeout map
  val tokenTimeoutMap : concurrent.Map[String, Long] = new ConcurrentHashMap[String, Long]().asScala

  // the encrypted symmetric key for each friend
//  val friendPublicKeyMap : concurrent.Map[Int, concurrent.Map[Int, String]]
//  = new ConcurrentHashMap[Int, concurrent.Map[Int, String]]().asScala

  var friendPublicKeyMap : Map[Int, Map[Int, String]] = Map()
}
