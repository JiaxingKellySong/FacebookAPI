package node

class Profile (profile_name : String) {

  private var _name : String = profile_name
  private var _workplace : String = null
  private var _school : String = null
  private var _email : String = null
  private var _current_place : String = null
  private var _iv : String = null
  private var _encrypted_key : String = null
  private var _key_map : String = null

  def name = { _name }
  def name_= (value : String) : Unit = { _name = value }

  def workplace = _workplace
  def workplace_=(value : String) : Unit = {_workplace = value}

  def school = _school
  def school_=(value : String) : Unit = {_school = value}

  def email = _email
  def email_=(value : String) : Unit = {_email = value}

  def current_place = _current_place
  def current_place_=(value : String) : Unit = {_current_place = value}

  def iv = _iv
  def iv_= (value : String) : Unit = { _iv = value }

  def encrypted_key = _encrypted_key
  def encrypted_key_=(value : String) : Unit = {_encrypted_key = value}

  def key_map = _key_map
  def key_map_=(value :String) : Unit = { _key_map = value }

}
