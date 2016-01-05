package node

/**
 * Created by win8 on 2015/11/25.
 */
class Picture (pic_id : Int) {
  private val _picture_id : Int = pic_id
  private var _album_id : Int = -1
  private var _creator_id : Int = -1
  private var _create_time : String = null
  private var _picture_data : String = ""
  private var _iv : String = null
  private var _encrypted_key : String = null
  private var _key_map : String = null

  def album_id = _album_id
  def album_id_=(value : Int) = {
    _album_id = value
  }

  def creator_id = _creator_id
  def creator_id_= (value : Int) = {
    _creator_id = value
  }

  def create_time = _create_time
  def create_time_=(value : String) = {
    _create_time = value
  }

  def picture_data = _picture_data
  def picture_data_=(value : String) = {
    _picture_data = value
  }

  def iv = _iv
  def iv_= (value : String) : Unit = { _iv = value }

  def encrypted_key = _encrypted_key
  def encrypted_key_=(value : String) : Unit = {_encrypted_key = value}

  def key_map = _key_map
  def key_map_=(value : String) : Unit = { _key_map = value }
}
