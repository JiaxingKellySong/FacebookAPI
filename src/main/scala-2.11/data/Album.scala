package data

import java.util.concurrent.ConcurrentHashMap

import node.Album
import org.json4s.native.Serialization._
import org.json4s.{FieldSerializer, DefaultFormats}

import scala.collection.concurrent
import scala.collection.convert.decorateAsScala._

/**
 * Created by win8 on 2015/11/25.
 */
object Album {
  var id = -1

  var albumMap : concurrent.Map[Int, Album] = new ConcurrentHashMap[Int, Album]().asScala

  private implicit val
  formats = DefaultFormats + FieldSerializer[Album]()
  def toJson(album: Album): String = writePretty(album)
}
