package data

import java.util.concurrent.ConcurrentHashMap

import node.Picture
import org.json4s.native.Serialization._
import org.json4s.{FieldSerializer, DefaultFormats}

import scala.collection.concurrent
import scala.collection.convert.decorateAsScala._

/**
 * Created by win8 on 2015/11/25.
 */
object Picture {

  var id : Int = -1

  var picMap : concurrent.Map[Int, Picture] = new ConcurrentHashMap[Int, Picture]().asScala

  private implicit val
  formats = DefaultFormats + FieldSerializer[Picture]()
  def toJson(picture : Picture): String = writePretty(picture)
}
