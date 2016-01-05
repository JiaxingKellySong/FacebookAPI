package data

import org.json4s.native.Serialization._
import org.json4s.{DefaultFormats, FieldSerializer}


object Profile {

  private implicit val formats = DefaultFormats + FieldSerializer[node.Profile]()
  def toJson(profile: node.Profile): String = writePretty(profile)
}
