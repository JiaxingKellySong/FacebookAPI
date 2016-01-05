package node

class FriendList {

  private var _listSize : Int = 0
  private var _idList = List[Int]()

  def listSize = _listSize
  def listSize_= (value : Int) : Unit = {
    _listSize = value
  }

  def idList = _idList
  def idList_= (value : List[Int]) = {
    _idList = value
  }
}
