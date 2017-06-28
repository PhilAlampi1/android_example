package com.getmemechat.memechat;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler extends SQLiteOpenHelper {

	// Database Version
	private static final int DATABASE_VERSION = 32;
	MyApplication mApp = (MyApplication) MyApplication.getContext();

	// Database Name
	private static final String DATABASE_NAME = "MessageDatabase";

	// Messages table name
	private static final String TABLE_MESSAGES = "messages";

	// Messages Table Columns names
	private static final String KEY_ID = "id";
	private static final String KEY_MSG_TXT = "msg_txt";
	private static final String KEY_IMG_URI = "img_uri";
	private static final String KEY_OTHER_USR_ID = "other_usr_id";
	private static final String KEY_OTHER_USR_PHONE = "other_usr_phone";
	private static final String KEY_OTHER_USR_NAME = "other_usr_name";
	private static final String KEY_LOCAL_USR_PHONE = "local_usr_phone";
	private static final String KEY_MSG_DATE_TIME = "msg_date_time";
	private static final String KEY_SENDER_PHONE = "sender_phone";
	private static final String KEY_MSG_STATUS = "msg_status";

	public DatabaseHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	// Message Status Codes
	private static final int READY_TO_SEND = 1;
	private static final int SENT_TO_SERVER = 2;
	private static final int FAILED_SERVER_SEND = 3;
	private static final int SENT_TO_OTHER_USER = 4;
	private static final int RECEIVED_BY_OTHER_USER = 5; // Notification received
	private static final int FAILED_SEND_TO_OTHER_USER = 6; // Never implemented
	private static final int OPENED_BY_OTHER_USER = 7; // Viewed in convo screen
	private static final int DELETED_ON_SERVER = 8;

	// Creating Tables
	@Override
	public void onCreate(SQLiteDatabase db) {

		String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_MESSAGES + "("
		+ KEY_ID + " INTEGER PRIMARY KEY," + KEY_MSG_TXT + " TEXT,"
		+ KEY_IMG_URI + " TEXT," + KEY_OTHER_USR_ID + " INTEGER,"
		+ KEY_OTHER_USR_PHONE + " TEXT," + KEY_OTHER_USR_NAME
		+ " TEXT," + KEY_MSG_DATE_TIME + " BIGINT, " + KEY_LOCAL_USR_PHONE
		+ " TEXT, " + KEY_SENDER_PHONE + " TEXT, " + KEY_MSG_STATUS + " INTEGER)";

		db.execSQL(CREATE_MESSAGES_TABLE);
	}

	// Upgrading database
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		// Drop older table if existed
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);

		// Create tables again
		onCreate(db);
	}

	/* All CRUD Operations */

	// Adding new message
	long addMessage(MsgDb message) {
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(KEY_MSG_TXT, message.getMsg());
		values.put(KEY_IMG_URI, message.getImgUri());
		values.put(KEY_OTHER_USR_NAME, message.getOtherUserName());
		values.put(KEY_LOCAL_USR_PHONE, message.getLocalUserPhone());
		values.put(KEY_OTHER_USR_ID, message.getOtherUserId());
		values.put(KEY_OTHER_USR_PHONE, message.getOtherUserPhone());
		values.put(KEY_MSG_DATE_TIME, message.getMsgDateTime());
		values.put(KEY_SENDER_PHONE, message.getSenderPhone());
		values.put(KEY_MSG_STATUS, message.getMsgStatus());

		// Inserting Row
		long id = db.insert(TABLE_MESSAGES, null, values);
		db.close(); // Closing database connection

		return id;
	}

	//to validate columns
	public void getColumns (){
		String selectQuery = "SELECT * FROM " + TABLE_MESSAGES;
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor c = db.rawQuery(selectQuery, null);
		try {
			String[] columnNames = c.getColumnNames();
		} finally {
			c.close();
		}
		db.close();
	}

	// view name and local user phone for selected record
	public String[] getLocalUserPhones (int other_usr_id){
		String[] luPhones = {"","",""};
		String selectQuery = "SELECT " + KEY_OTHER_USR_NAME + ", " + KEY_LOCAL_USR_PHONE + ", " + KEY_SENDER_PHONE + " FROM " + TABLE_MESSAGES + " WHERE " + KEY_OTHER_USR_ID + " = " + other_usr_id;
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// loop through first row, get other user name and phone
		if (cursor.moveToFirst()) {
			luPhones[0] = cursor.getString(0);
			luPhones[1] = cursor.getString(1);
			luPhones[2] = cursor.getString(2);
		}
		cursor.close();
		db.close();
		return luPhones;
	}

	//get other user name using ID
	public String[] getOtherUserInfo (String other_usr_phone){
		String[] ouInfo = {"","",""};
		String selectQuery = "SELECT " + KEY_OTHER_USR_NAME + ", " + KEY_OTHER_USR_PHONE  + " FROM " + TABLE_MESSAGES + " WHERE " + KEY_OTHER_USR_PHONE + " = " + other_usr_phone;
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// loop through first row, get other user name and phone
		if (cursor.moveToFirst()) {
			ouInfo[0] = cursor.getString(0);
			ouInfo[1] = cursor.getString(1);
		}
		cursor.close();
		db.close();
		return ouInfo;
	}

	//get all messages for a conversation based on other_usr_id passed in
	public List<MsgDb> getConvoMessages(String oup) {
		List<MsgDb> messageList = new ArrayList<MsgDb>();
		String selectQuery = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " + KEY_OTHER_USR_PHONE + " = " + oup;
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// Loop through all rows and add to list
		if (cursor.moveToFirst()) {
			do {
				MsgDb message = new MsgDb();
				message.setID(Integer.parseInt(cursor.getString(0)));
				message.setMsg(cursor.getString(1));
				message.setImgUri(cursor.getString(2));
				message.setOtherUserId(Integer.parseInt(cursor.getString(3)));
				message.setOtherUserPhone(cursor.getString(4));
				message.setOtherUserName(cursor.getString(5));
				message.setLocalUserPhone(cursor.getString(7));
				message.setMsgDateTime(cursor.getLong(6));
				message.setSenderPhone(cursor.getString(8));
				message.setMsgStatus(Integer.parseInt(cursor.getString(9)));
				messageList.add(message);
			} while (cursor.moveToNext());
		}
		cursor.close(); // added based on error received 12/31/2014
		db.close();
		return messageList;
	}

	//get all messages for a conversation based on other_usr_id passed in AND where unread
	public List<MsgDb> getUnreadConvoMessages(String oup) {
		List<MsgDb> messageList = new ArrayList<MsgDb>();
		String selectQuery = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " + KEY_SENDER_PHONE + " = " + oup +
		" AND " + KEY_MSG_STATUS + " != " + OPENED_BY_OTHER_USER;
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// Looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				MsgDb message = new MsgDb();
				message.setID(Integer.parseInt(cursor.getString(0)));
				message.setMsg(cursor.getString(1));
				message.setImgUri(cursor.getString(2));
				message.setOtherUserId(Integer.parseInt(cursor.getString(3)));
				message.setOtherUserPhone(cursor.getString(4));
				message.setOtherUserName(cursor.getString(5));
				message.setLocalUserPhone(cursor.getString(7));
				message.setMsgDateTime(cursor.getLong(6));
				message.setSenderPhone(cursor.getString(8));
				message.setMsgStatus(Integer.parseInt(cursor.getString(9)));
				messageList.add(message);
			} while (cursor.moveToNext());
		}
		cursor.close();
		db.close();
		return messageList;
	}

	// Get latest message across all users
	public List<MsgDb> getLatestMessages() {
		List<MsgDb> messageList = new ArrayList<MsgDb>();

		String selectQuery = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " + KEY_ID + " IN " +
		"(SELECT MAX (" + KEY_ID + ") FROM " + TABLE_MESSAGES +
		" GROUP BY " + KEY_OTHER_USR_PHONE + ") ORDER BY " + KEY_MSG_DATE_TIME + " DESC";

		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				MsgDb message = new MsgDb();
				message.setID(Integer.parseInt(cursor.getString(0)));
				message.setMsg(cursor.getString(1));
				message.setImgUri(cursor.getString(2));
				message.setOtherUserId(Integer.parseInt(cursor.getString(3)));
				message.setOtherUserPhone(cursor.getString(4));
				message.setOtherUserName(cursor.getString(5));
				message.setLocalUserPhone(cursor.getString(7));
				message.setMsgDateTime(cursor.getLong(6));
				message.setSenderPhone(cursor.getString(8));
				message.setMsgStatus(Integer.parseInt(cursor.getString(9)));

				// Adding contact to list
				messageList.add(message);
			} while (cursor.moveToNext());
		}
		cursor.close();
		db.close();
		return messageList;
	}

	// Update message status
	public int updateMsgStatus (int msg_id, int status) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(KEY_MSG_STATUS, status);

		// updating row
		return db.update(TABLE_MESSAGES, values, KEY_ID + " = ?",
		new String[] { String.valueOf(msg_id) });
	}

	// Update single contact
	public int updateMessage(MsgDb message) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(KEY_MSG_TXT, message.getMsg());
		values.put(KEY_IMG_URI, message.getImgUri());
		values.put(KEY_OTHER_USR_NAME, message.getOtherUserName());
		values.put(KEY_LOCAL_USR_PHONE, message.getLocalUserPhone());
		values.put(KEY_OTHER_USR_ID, message.getOtherUserId());
		values.put(KEY_OTHER_USR_PHONE, message.getOtherUserPhone());
		values.put(KEY_MSG_DATE_TIME, message.getMsgDateTime());
		values.put(KEY_SENDER_PHONE, message.getSenderPhone());
		values.put(KEY_MSG_STATUS, message.getMsgStatus());

		// updating row
		return db.update(TABLE_MESSAGES, values, KEY_ID + " = ?",
		new String[] { String.valueOf(message.getID()) });
	}

	// Update status of contacts
	public int updateStatus (String other_usr_phone, int status){
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(KEY_MSG_STATUS, status);

		// updating rows
		return db.update(TABLE_MESSAGES, values, KEY_OTHER_USR_PHONE + " = ? AND " + KEY_MSG_STATUS + " != ? "
		+ "AND " + KEY_SENDER_PHONE + " != ?",
		new String[] { other_usr_phone, String.valueOf(7), mApp.getGlobalLocalUsrPhone()});
	}

	// Delete single message
	public void deleteMessage (MsgDb message) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_MESSAGES, KEY_ID + " = ?",
		new String[] { String.valueOf(message.getID()) });
		db.close();
	}

	// Get messages count
	public int getMessagesCount() {
		String countQuery = "SELECT * FROM " + TABLE_MESSAGES;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, null);
		cursor.close();
		db.close();

		return cursor.getCount();
	}
}
