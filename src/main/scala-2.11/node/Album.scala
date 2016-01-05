package node

/**
 * Created by win8 on 2015/11/25.
 */
class Album (aid : Int) {

  private val album_id : Int = aid
  private var _creator_id : Int = -1
  private var _create_time : String = null
  private var _pic_id_list = List[Int]()
  private var _key_map : String = null

  def creator_id = _creator_id
  def creator_id_= (value : Int) = {
    _creator_id = value
  }

  def create_time = _create_time
  def create_time_= (value : String) = {
    _create_time = value
  }

  def pic_id_list = _pic_id_list
  def pic_id_list_=( value : List[Int]) = {
    _pic_id_list = value
  }

  def key_map = _key_map
  def key_map_=(value : String) : Unit = { _key_map = value }
}
