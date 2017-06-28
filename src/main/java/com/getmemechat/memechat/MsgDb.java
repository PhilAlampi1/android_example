package com.getmemechat.memechat;

public class MsgDb {

	// private variables
	long _id;
	String _msg_txt;
	String _img_uri;
	int _other_usr_id;
	String _other_usr_phone;
	String _other_usr_name;
	String _local_usr_phone;
	Long _msg_date_time;
	String _sender_phone;
	int _msg_status;

	// Empty constructor
	public MsgDb() {

	}

	// constructor
	public MsgDb(long id, String msg_txt, String img_uri, int other_usr_id,
			String other_usr_phone, String other_usr_name, String local_usr_phone, Long msg_date_time, String sender_phone, int msg_status) {
		this._id = id;
		this._msg_txt = msg_txt;
		this._img_uri = img_uri;
		this._other_usr_id = other_usr_id;
		this._other_usr_phone = other_usr_phone;
		this._other_usr_name = other_usr_name;
		this._local_usr_phone = local_usr_phone;
		this._msg_date_time = msg_date_time;
		this._sender_phone = sender_phone;
		this._msg_status = msg_status;
	}

	// getting ID
	public long getID() {
		return this._id;
	}

	// setting id
	public void setID(long id) {
		this._id = id;
	}

	// getting name
	public String getMsg() {
		return this._msg_txt;
	}

	// setting name
	public void setMsg(String msg) {
		this._msg_txt = msg;
	}

	public String getImgUri() {
		return this._img_uri;
	}

	public void setImgUri(String uri) {
		this._img_uri = uri;
	}

	public String getOtherUserName() {
		return this._other_usr_name;
	}

	public void setOtherUserName(String name) {
		this._other_usr_name = name;
	}

	public String getLocalUserPhone() {
		return this._local_usr_phone;
	}

	public void setLocalUserPhone(String p) {
		this._local_usr_phone = p;
	}

	public int getOtherUserId() {
		return this._other_usr_id;
	}

	public void setOtherUserId(int id) {
		this._other_usr_id = id;
	}

	public String getOtherUserPhone() {
		return this._other_usr_phone;
	}

	public void setOtherUserPhone(String p) {
		this._other_usr_phone = p;
	}

	public Long getMsgDateTime(){
		return this._msg_date_time;
	}

	public void setMsgDateTime(Long mdt){
		this._msg_date_time = mdt;
	}

	public String getSenderPhone (){
		return this._sender_phone;
	}

	public void setSenderPhone (String p){
		this._sender_phone = p;
	}
	public int getMsgStatus() {
		return this._msg_status;
	}

	public void setMsgStatus(int s) {
		this._msg_status = s;
	}
}
