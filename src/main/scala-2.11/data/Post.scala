package data

import java.util.concurrent.ConcurrentHashMap

import node.Post
import org.json4s.native.Serialization._
import org.json4s.{DefaultFormats, FieldSerializer}

import scala.collection._
import scala.collection.convert.decorateAsScala._


object Post {
  var id = -1

  var postMap : concurrent.Map[Int, Post] = new ConcurrentHashMap[Int, Post]().asScala

  private implicit val
  formats = DefaultFormats + FieldSerializer[Post]()
  def toJson(post: Post): String = writePretty(post)
}
