package com.getmemechat.memechat;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.MenuItemCompat.OnActionExpandListener;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FragConvoStream extends Fragment implements TextView.OnEditorActionListener {

	private ArrayList<FlickrPhoto> mPhotos = new ArrayList<FlickrPhoto>();
	public final static String API_KEY = "YOUR_FLICKR_KEY_HERE";
	public final static String NUM_PHOTOS = "100";
	protected static final int DEFINITION = 5;
	String searchTags;
	public long startTime1 = System.currentTimeMillis();
	public long elapsedTime1 = 0;
	private MenuItem myActionMenuItem;
	private EditText myActionEditText;
	boolean showSendBtn = true;
	public final int PICK_CONTACT_REQUEST = 1;
	MyApplication mApp = (MyApplication) MyApplication.getContext();
	public int spaceCount = 0;
	public String otherUP;
	public ProgressBar pb;
	List<MsgDb> messages;
	List<MsgDb> newMessages;
	String phone;
	CustomListConvo imageAdapter;
	ListView listView;
	String img_uri;
	private static final int OPENED_BY_OTHER_USER = 7;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(com.getmemechat.memechat.R.layout.frag_convo_stream, container, false);
		setHasOptionsMenu(true);

		// Receive other user id from Main Activity passConvo method
		Bundle args = getArguments();
		if (args != null) {
			otherUP = (args.getString("OUP"));
		}

		// Initiate ListView
		listView = (ListView) v.findViewById(R.id.listConvo);
		View footerView = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.frag_create_msg, null,
		false);
		pb = (ProgressBar) footerView.findViewById(R.id.pbConvo);
		pb.setIndeterminate(true);
		pb.setVisibility(View.GONE);
		listView.addFooterView(footerView);

		// Open db, run query and store in array of MsgDb objects
		DatabaseHandler db = new DatabaseHandler(getActivity());
		messages = db.getConvoMessages(otherUP);
		db.close();

		updateConvoMsgsToRead(otherUP);

		// Verify data (remove in final code)
		for (MsgDb cn : messages) {
			String log = "msg: " + cn.getMsg() + " ,Sender Phone: " + cn.getSenderPhone() + " ,Other User ID: " + cn.getOtherUserId()
			+ " ,Other User Name: " + cn.getOtherUserName() + " ,URI: " + cn.getImgUri() + " ,Date/Time: " + cn.getMsgDateTime()
			+ " ,OtherUserPhone: " + cn.getOtherUserPhone() + " ,LocalUserPHone: " + cn.getLocalUserPhone() + " ,Status: "
			+ cn.getMsgStatus();
		}

		// Pass array of MsgDb objects to Adapter
		CustomListConvo imageAdapter = new CustomListConvo(this.getActivity(), R.layout.convo_list_single, messages);
		listView.setAdapter(imageAdapter);

		// Set listener for long press
		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {

				if (messages.get(position)._img_uri != null) {
					// Prompt if user wants to make this their default
					// background
					AlertDialog.Builder builder = new AlertDialog.Builder(mApp.getGlobalMainContext());
					builder.setTitle("Just checking...");
					builder.setMessage("Make this your default background?");

					// If so, set it as the default background in shared
					// preferences
					builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							mApp.getGlobalMainContext();
							SharedPreferences.Editor editor = mApp.getGlobalMainContext().getSharedPreferences("SharedPrefs", Context.MODE_PRIVATE)
							.edit();
							editor.putString("Image", messages.get(position)._img_uri);
							editor.commit();
							dialog.dismiss();
							Toast.makeText(mApp.getGlobalMainContext(), "Default background updated.", Toast.LENGTH_SHORT).show();
						}
					});

					// If not, do nothing
					builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// Do nothing
							dialog.dismiss();
						}
					});
					AlertDialog alert = builder.create();
					alert.show();
				}
				return true;
			}
		});

		// Set onboarding alert message for entering message
		String ckBoxName = "cbMsg";
		String title = "To send a message...";
		CharSequence msg = Html.fromHtml("Type your message on the screen " +
		"then select the magnifying glass at the top to search for background images.");
		mApp.sendOnboardingAlert(ckBoxName, title, msg);
		return v;
	}

	public boolean isOnline() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		return (networkInfo != null && networkInfo.isConnected());
	}

	private class LoadPhotos extends AsyncTask<String, String, Long> {
		@Override
		protected void onPreExecute() {
			pb.setVisibility(View.VISIBLE);
		}

		@Override
		protected void onPostExecute(Long result) {
			if (result == 0) {
				pb.setVisibility(View.GONE);

				// resize viewpager height to = width of screen
				DisplayMetrics metrics = new DisplayMetrics();
				getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
				View viewPager = (View) getActivity().findViewById(com.getmemechat.memechat.R.id.pager);
				LayoutParams params = viewPager.getLayoutParams();
				params.height = metrics.widthPixels;
				viewPager.setLayoutParams(params);
				viewPager.setVisibility(View.VISIBLE);

				// set viewpager
				Intent i = getActivity().getIntent();
				int position = i.getIntExtra("position", 0);
				ImageAdapter adapter = new ImageAdapter(getActivity(), mPhotos);
				((ViewPager) viewPager).setAdapter(adapter);
				((ViewPager) viewPager).setCurrentItem(position);

				viewPager.setOnTouchListener(new View.OnTouchListener() {
					public boolean onTouch(View v, MotionEvent event) {

						if(event.getAction() == MotionEvent.ACTION_MOVE && listView!=null){
							listView.requestDisallowInterceptTouchEvent(true);
						}
						return false;
					}
				});

				// Set onboarding alert message for swiping and sending images
				String ckBoxName = "cbSwipe";
				String title = "To select your background...";
				CharSequence msg = Html.fromHtml("Almost done! Swipe through images until you find " +
				"the background you'd like to use. When ready to send, press the " +
				"send icon at the top of the screen.");
				mApp.sendOnboardingAlert(ckBoxName, title, msg);

			} else {
				Toast.makeText(getActivity().getApplicationContext(), "Please connect to the internet to retrieve a background", Toast.LENGTH_SHORT)
				.show();
			}
		}

		@Override
		protected Long doInBackground(String... params) {
			HttpURLConnection connection = null;
			try {
				URL dataUrl = new URL("https://api.flickr.com/services/rest/?&method=flickr.photos.search&api_key=" + API_KEY + "&per_page="
				+ NUM_PHOTOS + "&orientation=square" + "&format=json" + "&nojsoncallback=1" + "&tags=" + searchTags);
				connection = (HttpURLConnection) dataUrl.openConnection();
				connection.connect();
				int status = connection.getResponseCode();
				//Log.d("connection", "status " + status);
				if (status == 200) {
					InputStream is = connection.getInputStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(is));
					String responseString;
					StringBuilder sb = new StringBuilder();
					while ((responseString = reader.readLine()) != null) {
						sb = sb.append(responseString);
					}
					String photoData = sb.toString();
					mPhotos = FlickrPhoto.makePhotoList(photoData);
					//Log.d("connection", photoData);
					mApp.setGlobalMsgObj(mPhotos);
					return (0l);
				} else {
					return (1l);
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return (1l);
			} catch (IOException e) {
				System.out.println("IOException");
				e.printStackTrace();
				return (1l);
			} catch (NullPointerException e) {
				e.printStackTrace();
				return (1l);
			} catch (JSONException e) {
				e.printStackTrace();
				return (1l);
			} finally {
				connection.disconnect();
			}
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.create_msg_menu, (android.view.Menu) menu);

		// Get the action view
		myActionMenuItem = menu.findItem(R.id.action_search);
		View actionView = myActionMenuItem.getActionView();

		// Get the edit text view that is part of the action view
		if (actionView != null) {
			myActionEditText = (EditText) actionView.findViewById(R.id.myActionEditText);

			// Set a listener that will be called when the return/enter
			// key is pressed
			if (myActionEditText != null) {
				myActionEditText.setOnEditorActionListener(this);
			}
		}
		// For support of API level 14 and below, use MenuItemCompat
		MenuItemCompat.setOnActionExpandListener(myActionMenuItem, new OnActionExpandListener() {
			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				return true;
			}

			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {

				// Set onboarding alert message for searching images
				String ckBoxName = "cbSearch";
				String title = "To search images...";
				CharSequence msg = Html.fromHtml("Getting there! Type in one or more search terms then" +
				" press Enter on your keyboard to start the search.");
				mApp.sendOnboardingAlert(ckBoxName, title, msg);

				// Set actions when expanded
				if (myActionEditText != null) {
					myActionEditText.setText("");

					// request focus when item is expanded,
					// automatically pop up keyboard
					myActionEditText.requestFocus();
					myActionEditText.post(new Runnable() {
						@Override
						public void run() {
							showSoftInputUnchecked();
						}
					});
				}

				return true;
			}
		});

	}

	@Override
	public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
		if (keyEvent != null) {

			// handle search event when user is searching for a different
			// background
			if (textView.getId() == R.id.myActionEditText) {
				if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
					// CharSequence textInput = textView.getText();
					searchTags = textView.getText().toString();
					searchTags = searchTags.replace(" ", "");
					if (isOnline()) {
						LoadPhotos task = new LoadPhotos();
						task.execute();
					} else {
						Toast.makeText(getActivity().getApplicationContext(), "Please connect to the internet to retrieve a background",
						Toast.LENGTH_SHORT).show();
					}
					MenuItemCompat.collapseActionView(myActionMenuItem);
				}
			}
		}
		return true;
	}

	@Override
	public void onPause() {
		super.onPause();
		mApp.setGlobalConvoOUP("");
	}

	@Override
	public void onResume() {
		super.onResume();
		mApp.setGlobalConvoOUP(otherUP);

		// Set title
		String title = mApp.getGlobalDisplayName();
		getActivity().getActionBar().setTitle(title);
		((MainActivity) getActivity()).setActionBarTitle(title);

		// If there are pending notifications
		if (mApp.getGlobalNotificationCounter() > 0) {

			// Update records and dismiss notifications
			updateConvoMsgsToRead(otherUP);

			// Reload all data for this convo into adapter and reload listview
			DatabaseHandler db = new DatabaseHandler(getActivity());
			messages = db.getConvoMessages(otherUP);
			CustomListConvo imageAdapter = new CustomListConvo(mApp.getGlobalMainContext(), R.layout.convo_list_single, messages);
			listView.setAdapter(imageAdapter);
			imageAdapter.notifyDataSetChanged();
			db.close();
		}
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		// hide new button
		MenuItem item = menu.findItem(R.id.action_new);
		item.setVisible(false);

		// set title
		String title = mApp.getGlobalDisplayName();
		getActivity().getActionBar().setTitle(title);
		((MainActivity) getActivity()).setActionBarTitle(title);
	}

	private void showSoftInputUnchecked() {
		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

		if (imm != null) {
			Method showSoftInputUnchecked = null;
			try {
				showSoftInputUnchecked = imm.getClass().getMethod("showSoftInputUnchecked", int.class, ResultReceiver.class);
			} catch (NoSuchMethodException e) {
				// Log something
			}

			if (showSoftInputUnchecked != null) {
				try {
					showSoftInputUnchecked.invoke(imm, 0, null);
				} catch (IllegalAccessException e) {
					// Log something
				} catch (InvocationTargetException e) {
					// Log something
				}
			}
		}
	}

	// Used to update list when send clicked in ActionBar (MainActivity) and many other places
	public void reloadData(MsgDb newMsg, Context c) {

		// Add new message to existing message object being used in ListView
		messages.add(newMsg);
		CustomListConvo imageAdapter = new CustomListConvo(c, R.layout.convo_list_single, messages);
		listView.setAdapter(imageAdapter);
		imageAdapter.notifyDataSetChanged();

		// If local user sent the msg, clear out ViewPager and EditText
		if (newMsg.getSenderPhone().equals(mApp.getGlobalLocalUsrPhone())) {
			View viewPager = (View) getActivity().findViewById(com.getmemechat.memechat.R.id.pager);
			viewPager.setVisibility(View.GONE);
			EditText et1 = (EditText) getActivity().findViewById(R.id.editText1);
			et1.setText("");
		}
		dismissNotification((int) newMsg.getID());
		return;
	}

	public void dismissNotification (int id){
		NotificationManager mNotificationManager = (NotificationManager) mApp.getGlobalMainContext().getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancelAll();
		if (mApp.getGlobalNotificationCounter() > 0)
		mApp.setGlobalNotificationCounter(0);
		mApp.setGlobalNotificationId(0);
	}

	public void updateConvoMsgsToRead(String oui) {

		// Set status of all messages to indicate the user has opened them (OPENED_BY_OTHER_USER = 7)
		DatabaseHandler db2 = new DatabaseHandler(getActivity());
		newMessages = db2.getUnreadConvoMessages(otherUP);

		// Dismiss notifications for new messages
		if (newMessages.size() > 0){
			for (MsgDb m : newMessages){
				dismissNotification((int) m.getID());
			}
		}
		int success = db2.updateStatus(oui, OPENED_BY_OTHER_USER);
		db2.close();
	}
}
