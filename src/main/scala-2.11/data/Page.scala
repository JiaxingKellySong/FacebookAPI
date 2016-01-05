import java.util.concurrent.ConcurrentHashMap

import node.{FriendList, Page, Profile}
import org.json4s.native.Serialization._
import org.json4s.{DefaultFormats, FieldSerializer}

import scala.collection._
import scala.collection.convert.decorateAsScala._

object Page {
  // automatically increment id
  var id = -1

  var pagesMap : concurrent.Map[Int, Page] = new ConcurrentHashMap[Int, Page]().asScala

  private implicit val
  formats = DefaultFormats + FieldSerializer[Page]() + FieldSerializer[Profile]() + FieldSerializer[FriendList]()
  def toJson(page: Page): String = writePretty(page)

  def areFriend(id0 : Int, id1 : Int) : Boolean = {
    Page.pagesMap.contains(id0) &&
      Page.pagesMap.get(id0).orNull.friendlist.idList.contains(id1)
  }
}