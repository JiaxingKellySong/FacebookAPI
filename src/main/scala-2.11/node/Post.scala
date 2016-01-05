package node

class Post (pid : Int) {

  private var _post_id : Int = pid
  private var _creator_id : Int = -1
  private var _create_time : String = null
  private var _message : String = null
  private var _iv : String = null
  private var _encrypted_key : String = null
  private var _key_map : String = null

  def creator_id = _creator_id
  def creator_id_= (value : Int) : Unit = {
    _creator_id = value
  }

  def create_time = _create_time
  def create_time_= (value : String) : Unit = {
    _create_time = value
  }

  def message = _message
  def message_= (value : String) : Unit = {
    _message = value
  }

  def iv = _iv
  def iv_= (value : String) : Unit = { _iv = value }

  def encrypted_key = _encrypted_key
  def encrypted_key_=(value : String) : Unit = {_encrypted_key = value}

  def key_map = _key_map
  def key_map_=(value : String) : Unit = { _key_map = value }
}
