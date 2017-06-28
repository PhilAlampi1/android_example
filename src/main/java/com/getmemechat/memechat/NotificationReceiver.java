package com.getmemechat.memechat;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import com.parse.ParsePushBroadcastReceiver;
import org.json.JSONException;
import org.json.JSONObject;

public class NotificationReceiver extends ParsePushBroadcastReceiver {

	// Message Status Codes
	private static final int READY_TO_SEND = 1;
	private static final int SENT_TO_SERVER = 2;
	private static final int FAILED_SERVER_SEND = 3;
	private static final int SENT_TO_OTHER_USER = 4;
	private static final int RECEIVED_BY_OTHER_USER = 5;
	private static final int FAILED_SEND_TO_OTHER_USER = 6;
	private static final int OPENED_BY_OTHER_USER = 7;
	private static final int DELETED_ON_SERVER = 8;

	static MyApplication mApp = (MyApplication) MyApplication.getContext();

	@Override
	public void onPushOpen(Context context, Intent intent) {
		Intent i = new Intent(context, MainActivity.class);
		i.putExtras(intent.getExtras());
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(i);
	}

	@Override
	protected void onPushReceive(Context mContext, Intent intent) {

		// Unpack JSON object into MsgDb object
		try {
			String action = intent.getAction();

			JSONObject json = new JSONObject(intent.getExtras().getString(
			"com.parse.Data"));
			if (action.equalsIgnoreCase("com.parse.push.intent.RECEIVE")) {

				// load all JSON values - remove MsgTxt from send if not needed
				// (e.g. if alert can carry long strings over 140 char)
				String msg_txt = "";
				String img_uri = "";
				String local_usr_phone = "";
				String ou_phone = "";
				String sender_phone = "";
				String toDisplayName = "";
				int other_usr_id = 0;

				if (json.has("alert")) {
					msg_txt = json.getString("alert");
				}
				if (json.has("ImgUri")) {
					img_uri = json.getString("ImgUri");
				}
				if (json.has("OtherUsrPhone")) {
					local_usr_phone = json.getString("OtherUsrPhone");
				} // switched other usr phone with local usr phone - app design
				// depends on this
				if (json.has("LocalUsrPhone")) {
					ou_phone = json.getString("LocalUsrPhone");
				} // switched other usr phone with local usr phone - app design
				// depends on this
				if (json.has("SenderPhone")) {
					sender_phone = json.getString("SenderPhone");
				}

				String[] sa = mApp.getPhoneAndUID(ou_phone);
				toDisplayName = sa[0];
				if (sa[1] != null){
					other_usr_id = Integer.valueOf(sa[1]);
				}

				// set msg_date_time
				Long msg_date_time = System.currentTimeMillis();

				DatabaseHandler db = new DatabaseHandler(mContext);
				MsgDb msgObj = new MsgDb(0, msg_txt, img_uri, other_usr_id,
				ou_phone, toDisplayName, local_usr_phone,
				msg_date_time, sender_phone, RECEIVED_BY_OTHER_USER);
				Long msgid = db.addMessage(msgObj);
				db.close();
				msgObj.setID(msgid);

				// generate notification and open in app when touched
				generateNotification(mApp.getApplicationContext(), msgObj); //mContext

				mApp.routeNotification(msgObj);
			}
		} catch (JSONException e) {
			Log.w("jsonexc", "JSONException: " + e.getMessage());
		}
	}

	private static void generateNotification(Context context, MsgDb m) {

		String title = "";
		String noteMsg = "";

		if (mApp.getGlobalNotificationCounter() == 0) { // First notification in series
			mApp.setGlobalNotificationCounter(mApp.getGlobalNotificationCounter() + 1);

			// Set title for notification
			if (m.getOtherUserName() != "") {
				title = m.getOtherUserName();
			} else {
				title = m.getSenderPhone();
			}

			// Set message for notification
			noteMsg = m.getMsg();

			// Set id for notification
			mApp.setGlobalNotificationId((int)m.getID());

		} else { // Subsequent notification in series
			mApp.setGlobalNotificationCounter(mApp.getGlobalNotificationCounter() + 1);
			title = "New Messages";
			noteMsg = "You have " + mApp.getGlobalNotificationCounter() + " new messages.";
		}

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
		context).setSmallIcon(R.drawable.ic_launcher)
		.setContentTitle(title)
		.setContentText(noteMsg)
		.setAutoCancel(true)
		.setPriority(Notification.PRIORITY_MAX)
		.setDefaults(-1);

		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(context, MainActivity.class);

		// The stack builder object will contain an artificial back stack for
		// the started Activity. This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(MainActivity.class);

		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
		PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager = (NotificationManager) context
		.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify((int) mApp.getGlobalNotificationId(), mBuilder.build());
	}
}
