package com.getmemechat.memechat;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.support.v4.view.ViewPager;
import android.telephony.SmsManager;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import com.getmemechat.memechat.FragMsgStream.DataPassListener;
import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class MainActivity extends Activity implements DataPassListener {

	public final int PICK_CONTACT_REQUEST = 1;
	MyApplication mApp = (MyApplication) MyApplication.getContext();
	ArrayList<FlickrPhoto> UriList;
	int other_usr_id;
	public String msg_txt = null;
	public String img_uri = null;
	public String phone = null;
	FragConvoStream fcs = new FragConvoStream();
	boolean sendLock = false;

	// App URL
	public String appUrl = "GetMemeChat.com";

	// Message Status Codes
	private static final int READY_TO_SEND = 1;
	private static final int SENT_TO_SERVER = 2;
	private static final int FAILED_SERVER_SEND = 3;
	private static final int SENT_TO_OTHER_USER = 4;
	private static final int RECEIVED_BY_OTHER_USER = 5; // Notification received
	private static final int FAILED_SEND_TO_OTHER_USER = 6; // Never implemented
	private static final int OPENED_BY_OTHER_USER = 7; // Viewed in convo screen
	private static final int DELETED_ON_SERVER = 8;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Show EULA
		new AppEULA(this).show();

		// Get width of screen and store globally
		DisplayMetrics metrics = new DisplayMetrics();
		this.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		mApp.setWidth(metrics.widthPixels);

		// Show Message Stream fragment
		Intent intent = getIntent();
		String action = intent.getAction();

		if (findViewById(R.id.layout_container_fms) != null) {
			if (savedInstanceState != null) {
				return;
			}
			removeCurrentFragment();
			FragMsgStream fms = new FragMsgStream();
			FragmentTransaction ft1 = getFragmentManager().beginTransaction();
			ft1.add(R.id.layout_container_fms, fms, "msg_stream");
			ft1.addToBackStack("msg_stream_layer");
			ft1.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			ft1.commit();
			mApp.setGlobalCurrentScreen("MsgStream");
		}
		mApp.setGlobalMainContext(this);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {

			// Clear out key variables
			msg_txt = null;
			mApp.setGlobalMsgObj(null);

			if (getFragmentManager().getBackStackEntryCount() == 0) {
				this.finish();
				return false;
			} else {
				getFragmentManager().popBackStack();
				removeCurrentFragment();

				// Go back to msgstream frag unless already there, then close
				FragMsgStream fms = new FragMsgStream();
				FragmentTransaction ft1 = getFragmentManager().beginTransaction();
				ft1.add(R.id.layout_container_fms, fms, "msg_stream");
				ft1.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
				ft1.commit();
				mApp.setGlobalCurrentScreen("MsgStream");
				this.setActionBarTitle("All Messages");
				return false;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	public void removeCurrentFragment() {

		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		Fragment currentFrag = getFragmentManager().findFragmentById(R.id.layout_container_fms);
		String fragName = "NONE";

		if (currentFrag != null)
		fragName = currentFrag.getClass().getSimpleName();

		if (currentFrag != null)
		transaction.remove(currentFrag);

		transaction.commit();
	}

	@Override
	public void onResume() {
		super.onResume();

		// Set title
		this.setActionBarTitle("All Messages");
		mApp.checkForNewMsgsOnFirstOpen();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, (android.view.Menu) menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// Handle presses on the action bar items
		switch (item.getItemId()) {
			case R.id.action_new:

			// launch contact intent
			pickContact();
			return true;
			case R.id.action_search:

			// Code you want run when activity is clicked
			return true;
			case R.id.action_send:

			// Lock functionality while processing to avoid duplicate submissions
			if (sendLock == false) {
				sendLock = true;
				// Set message text
				EditText et1 = (EditText) findViewById(R.id.editText1);
				msg_txt = et1.getText().toString();
				et1.setText(""); // To prevent rapid clicks sending multiple of the same msg

				// Set current image uri
				ViewPager vp = (ViewPager) findViewById(R.id.pager);
				int position = vp.getCurrentItem();
				UriList = mApp.getGlobalMsgObj();

				// Ensure ViewPager has something in it before proceeding
				if (UriList == null) { // New background was not selected

					// Since there's no background selected, check to ensure
					// there's at least some text
					// otherwise could be a duplicate push of the send button
					if (msg_txt.equalsIgnoreCase("")) {
						Toast.makeText(getApplicationContext(), "Please type a message and/or select a background.", Toast.LENGTH_LONG).show();
						sendLock = false;
						return false;
					}

					// See if a default background has been set, if not, provide
					// an image URL for it to use
					SharedPreferences prefs = getSharedPreferences("SharedPrefs", MODE_PRIVATE);
					String getSharedPrefs = prefs.getString("Image", null);
					if (getSharedPrefs != null) {
						img_uri = prefs.getString("Image", "http://farm9.staticflickr.com/8617/16724962256_618d013ca4_n.jpg");
					} else {
						img_uri = "http://farm9.staticflickr.com/8617/16724962256_618d013ca4_n.jpg";
					}
				} else { // New background was selected
					FlickrPhoto currentPhoto = UriList.get(position);
					String curUrl = currentPhoto.getPhotoUrl(false);
					img_uri = curUrl;
				}
				Long msg_date_time = System.currentTimeMillis(); // * -1;
				String dateString = new SimpleDateFormat("EEE, LLL dd hh:mm a").format(new Date(msg_date_time));

				// Set sender_phone = local_user_phone
				String sender_phone = mApp.getGlobalLocalUsrPhone();

				// Set toDisplayName and otherUserPhone (in case user is not
				// coming through new message button, instead is originating msg from convo screen)
				DatabaseHandler db1 = new DatabaseHandler(this);
				String[] s = db1.getOtherUserInfo(mApp.getGlobalOtherUsrPhone());
				String testName = s[0];
				String ouPhone = s[1];
				db1.close();
				if (ouPhone == "") {

					// Leave toDisplayName and phone as-is, this is a new convo
				} else {
					mApp.setGlobalDisplayName(testName);
					phone = mApp.formatPhoneNum(ouPhone);
				}

				// Set message status
				int msgStatus = READY_TO_SEND;
				MsgDb msgObj = new MsgDb(0, msg_txt, img_uri, 0, // other_usr_id
				phone, mApp.getGlobalDisplayName(), mApp.getGlobalLocalUsrPhone(), msg_date_time, sender_phone, msgStatus);
				sendMsgToServer(msgObj);

				// If Shared Prefs SMS set to no - preserve variables
				SharedPreferences prefs = getSharedPreferences("SharedPrefs", MODE_PRIVATE);
				int sp = prefs.getInt("SMS", 0);
				if (sp == 1) {
					msg_txt = null;
					mApp.setGlobalMsgObj(null);
				}
				sendLock = false;
			}

			// Set onboarding alert message for setting default background
			String ckBoxName = "cbBackGround";
			String title = "In a hurry?";
			CharSequence msg = Html.fromHtml("You can set a default background by long pressing " +
			"on one of the images you've already sent.");
			mApp.sendOnboardingAlert(ckBoxName, title, msg);

			return true;
			default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void setActionBarTitle(String title) {
		getActionBar().setTitle(title);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		// Check which request it is that we're responding to
		if (requestCode == PICK_CONTACT_REQUEST) {

			// Make sure the request was successful
			if (resultCode == RESULT_OK) {

				// Get the URI that points to the selected contact
				Uri contactUri = data.getData();
				String[] projection = { ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
					ContactsContract.Contacts._ID };

					// Consider using CursorLoader to perform the query in the
					// future - take it off main UI thread
					Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);
					cursor.moveToFirst();

					// Retrieve the other name - for use in action bar within fragment
					int column = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
					mApp.setGlobalDisplayName(cursor.getString(column));

					// Retrieve the other user id
					column = cursor.getColumnIndex(ContactsContract.Contacts._ID);
					other_usr_id = Integer.valueOf(cursor.getString(column));
					cursor.close();

					// Retrieve the phone number get the phone number id from the Uri
					String id = contactUri.getLastPathSegment();

					// Query the phone numbers for the selected phone number id
					Cursor c = getContentResolver().query(Phone.CONTENT_URI, null, Phone._ID + "=?", new String[] { id }, null);
					int phoneIdx = c.getColumnIndex(Phone.NUMBER);
					if (c.getCount() == 1) { // contact has a single phone number
						if (c.moveToFirst()) {
							phone = c.getString(phoneIdx);
							phone = mApp.formatPhoneNum(phone);
						} else {
							Log.w("FAILED!", "No results");
						}
					}
					mApp.setGlobalOtherUsrPhone(phone);
					passConvo(phone, mApp.getGlobalDisplayName());
				}
			}
		}

		private void pickContact() {
			Intent pickContactIntent = new Intent(Intent.ACTION_PICK, ContactsContract.PhoneLookup.CONTENT_FILTER_URI);
			pickContactIntent.setType(Phone.CONTENT_TYPE); // Show user only
			startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST);
		}

		public String getOtherUserPhone() {
			return phone;
		}

		public MainActivity getContext() {
			return this;
		}

		public void passConvo(String i, String s) {
			mApp.setGlobalOtherUsrPhone(i);

			// Remove existing transaction
			removeCurrentFragment();

			// Open convo frag using other user phone (i) - passed from FragMsgStream
			Bundle args = new Bundle();
			args.putString("OUP", i);
			fcs.setArguments(args);
			FragmentTransaction ft3 = getFragmentManager().beginTransaction();
			ft3.add(R.id.layout_container_fms, fcs, "msg_convo");
			if (getFragmentManager().getBackStackEntryCount() == 0) {
				ft3.addToBackStack("convo_layer");
			}
			ft3.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			ft3.commit();
			mApp.setGlobalDisplayName(s);
			mApp.setGlobalOtherUsrPhone(i);
			this.setActionBarTitle(mApp.getGlobalDisplayName());
			mApp.setGlobalCurrentScreen("ConvoStream");
		}

		@Override
		public boolean onPrepareOptionsMenu(Menu menu) {
			super.onPrepareOptionsMenu(menu);
			this.setActionBarTitle("All Conversations");
			return true;
		}

		protected boolean sendMsgToServer(final MsgDb m) {

			// Update status in the MsgDb object
			m.setMsgStatus(SENT_TO_SERVER);

			// See if other user is registered on app yet
			String oup = m.getOtherUserPhone();
			final HashMap<String, Object> params = new HashMap<String, Object>();
			params.put("oup", oup);
			ParseCloud.callFunctionInBackground("isRegistered", params, new FunctionCallback<String>() {
				public void done(String result, ParseException e) {
					if (e == null) {
						int r = 0;
						try {
							r = Integer.parseInt(result);
							if (r <= 0) { // Other user not on app yet

								// Check Shared Prefs - if previously agreed, do not prompt again just send text
								SharedPreferences prefs = getSharedPreferences("SharedPrefs", MODE_PRIVATE);
								int sp = prefs.getInt("SMS", 0);
								if (sp != 1) {

									// Prompt user about sending text message
									AlertDialog.Builder builder = new AlertDialog.Builder(mApp.getGlobalMainContext());
									builder.setTitle(m.getOtherUserName() + " isn't on " + getResources().getString( R.string.app_name) + " yet");
									builder.setMessage("We'll send your message as a text and invite them to join.");
									builder.setPositiveButton("Ok, do this from now on", new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int which) {

											// Insert message into db
											DatabaseHandler db = new DatabaseHandler(mApp.getGlobalMainContext());
											db.addMessage(m);
											db.close();

											// Refresh listview on convo screen
											FragConvoStream fcs = (FragConvoStream) getFragmentManager().findFragmentByTag("msg_convo");
											fcs.reloadData(m, mApp.getGlobalMainContext());

											// Send text
											sendSMS(m.getMsg(), m.getOtherUserPhone());

											// Update SharedPrefs not to prompt the user on texting again
											SharedPreferences.Editor editor = mApp.getGlobalMainContext()
											.getSharedPreferences("SharedPrefs", Context.MODE_PRIVATE).edit();
											editor.putInt("SMS", 1);
											editor.commit();
											dialog.dismiss();

											// Store on server
											storeOnServer(m);

											// Reset key variables here
											msg_txt = null;
											mApp.setGlobalMsgObj(null);
											dialog.dismiss();
										}
									});
									builder.setNegativeButton("Not now", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											Toast.makeText(getApplicationContext(),
											m.getOtherUserName() + " will not get your message until you try again and select OK.",
											Toast.LENGTH_LONG).show();
											dialog.dismiss();
										}
									});
									AlertDialog alert = builder.create();
									alert.show();
								} else {

									// No need to prompt user about sending text
									// message, SMS Shared Pref already exists as 1
									DatabaseHandler db = new DatabaseHandler(mApp.getGlobalMainContext());
									db.addMessage(m);
									db.close();

									// Refresh listview on convo screen
									FragConvoStream fcs = (FragConvoStream) getFragmentManager().findFragmentByTag("msg_convo");
									if (fcs != null)
									fcs.reloadData(m, mApp.getGlobalMainContext());

									// Send text
									sendSMS(m.getMsg(), m.getOtherUserPhone());

									// Store on server
									storeOnServer(m);
								}

							} else { // User already on app

								// Insert message into db
								DatabaseHandler db = new DatabaseHandler(mApp.getGlobalMainContext());
								db.addMessage(m);
								db.close();

								// Refresh listview on convo screen
								FragConvoStream fcs = (FragConvoStream) getFragmentManager().findFragmentByTag("msg_convo");
								fcs.reloadData(m, mApp.getGlobalMainContext());

								// Reset key variables here
								msg_txt = null;
								mApp.setGlobalMsgObj(null);

								// Create JSON object and send push
								JSONObject pushData;
								try {
									pushData = new JSONObject("{" + "\"alert\": \"" + m.getMsg() + "\","
									+ "\"ImgUri\": \"" + m.getImgUri() + "\"," + "\"OtherUsrPhone\": \"" + m.getOtherUserPhone() + "\","
									+ "\"LocalUsrPhone\": \"" + m.getLocalUserPhone() + "\"," + "\"SenderPhone\": \"" + m.getSenderPhone() + "\""
									+ "}");
									params.put("pushData", pushData);
									ParseCloud.callFunctionInBackground("sendPush", params, new FunctionCallback<String>() {
										public void done(String success, ParseException e) {
											if (e == null) {
												Log.w("PUSH SUCCESS", "" + success);
											} else {
												Log.w("PUSH FAIL", "" + e);
											}
										}
									});
								} catch (JSONException e1) {
									e1.printStackTrace();
								}
							}
						} catch (NumberFormatException nfe) {
						}
					} else {
						Log.w("CLOUD CODE ERROR", "" + e);
					}
				}
			});

			return true;
		}

		public void sendSMS(String sms, String phoneNo) {

			// Send text msg insert shameless plug if there's room
			if (sms.length() + appUrl.length() + 3 < 160)
			sms = sms.concat(" - " + appUrl);
			try {
				SmsManager smsManager = SmsManager.getDefault();
				smsManager.sendTextMessage(phoneNo, null, sms, null, null);
				Toast.makeText(getApplicationContext(), "Your text and invite have been sent.", Toast.LENGTH_SHORT).show();
			} catch (Exception er) {
				Toast.makeText(getApplicationContext(), "SMS failed, please try again later.", Toast.LENGTH_SHORT).show();
				er.printStackTrace();
			}
		}

		public void storeOnServer (MsgDb m){

			// Add MsgDb object to SendMsg table on server - used to persist data when no installation is found
			ParseObject sendMsg = new ParseObject("SendMsg");
			sendMsg.put("keyId", m.getID());
			sendMsg.put("keyMsgTxt", m.getMsg());
			sendMsg.put("keyImgUri", m.getImgUri());
			sendMsg.put("keyOtherUsrId", m.getOtherUserId());
			sendMsg.put("keyOtherUsrPhone", m.getOtherUserPhone());
			sendMsg.put("keyOtherUsrName", m.getOtherUserName());
			sendMsg.put("keyMsgDateTime", m.getMsgDateTime());
			sendMsg.put("keyLocalUsrPhone", m.getLocalUserPhone());
			sendMsg.put("keySenderPhone", m.getSenderPhone());
			sendMsg.put("keyMsgStatus", SENT_TO_SERVER);
			sendMsg.saveInBackground();
		}
	}
