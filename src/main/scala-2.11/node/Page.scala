package node

class Page (page_id : Int, name : String) {

  private var _id: Int = page_id
  private var _page_name : String = name
  private var _profile : Profile = new Profile(name)
  private var _friendlist : FriendList = new FriendList()
  private var _keyStringList = List[String]()
  private var _postIdList = List[Int]()

  def id = _id
  def id_= (value : Int) : Unit = {
    _id = value
  }

  def page_name = _page_name
  def page_name_= (value : String) : Unit = {
    _page_name = value
  }

  def profile = _profile
  def profile_= (value : Profile) : Unit = {
    _profile = value
  }

  def friendlist = _friendlist
  def friendlist_= (value : FriendList) : Unit = {
    _friendlist = value
  }

  def keyList = _keyStringList
  def keyList_= (value : List[String]) : Unit = {
    _keyStringList = value
  }

  def postIdList = _postIdList
  def postIdList_=(value : List[Int]) = {
    _postIdList = value
  }
}

