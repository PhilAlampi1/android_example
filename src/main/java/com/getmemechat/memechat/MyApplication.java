package com.getmemechat.memechat;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import java.util.ArrayList;
import java.util.List;

public class MyApplication extends Application {

  // Message Status Codes
  private static final int READY_TO_SEND = 1;
  private static final int SENT_TO_SERVER = 2;
  private static final int FAILED_SERVER_SEND = 3;
  private static final int SENT_TO_OTHER_USER = 4;
  private static final int RECEIVED_BY_OTHER_USER = 5; // Notification received
  private static final int FAILED_SEND_TO_OTHER_USER = 6; // Never implemented
  private static final int OPENED_BY_OTHER_USER = 7; // Viewed in convo screen
  private static final int DELETED_ON_SERVER = 8;

  // Global variables for managing convo for routeNotification function below
  public String globalConvoOUP;
  public void setGlobalConvoOUP(String i) {
    globalConvoOUP = i;
  }
  public String getGlobalConvoOUP() {
    return globalConvoOUP;
  }

  // Display name variables
  public String globalDisplayName;
  public void setGlobalDisplayName(String i) {
    globalDisplayName = i;}
    public String getGlobalDisplayName() {
      return globalDisplayName;}

      // Global variables for managing notifications
      public int globalNotificationId = 0;
      public void setGlobalNotificationId(int i) {globalNotificationId = i;}
      public int getGlobalNotificationId() {return globalNotificationId;}
      public int globalNotificationCounter = 0;
      public void setGlobalNotificationCounter(int i) {
        globalNotificationCounter = i;
      }
      public int getGlobalNotificationCounter() {
        return globalNotificationCounter;
      }

      public String globalCurrentScreen = null;

      public void setGlobalCurrentScreen(String s) {
        globalCurrentScreen = s;
      }

      public String getGlobalCurrentScreen() {
        return globalCurrentScreen;
      }

      public ParseObject globalParseObject = null;

      public void setGlobalParseObject(ParseObject o) {
        globalParseObject = o;
      }

      public ParseObject getGlobalParseObject() {return globalParseObject;}

      public String globalOtherUsrPhone = "";

      public void setGlobalOtherUsrPhone(String i) {
        globalOtherUsrPhone = i;
      }

      public String getGlobalOtherUsrPhone() {
        return globalOtherUsrPhone;
      }

      public String globalLocalUsrPhone = null;

      public void setGlobalLocalUsrPhone(String s) {
        globalLocalUsrPhone = s;
      }

      public String getGlobalLocalUsrPhone() {
        if (globalLocalUsrPhone == null) {
          TelephonyManager tMgr = (TelephonyManager) mContext
          .getSystemService(Context.TELEPHONY_SERVICE);
          setGlobalLocalUsrPhone(tMgr.getLine1Number());
        }
        globalLocalUsrPhone = formatPhoneNum(globalLocalUsrPhone);
        return globalLocalUsrPhone;
      }

      private static Context mContext;

      public static Integer width;

      public void setWidth(int w) {
        width = w;
      }

      public Integer getWidth() {
        return width;
      }

      private ArrayList<FlickrPhoto> globalMsgObj = null;

      public ArrayList<FlickrPhoto> getGlobalMsgObj() {
        return globalMsgObj;
      }

      public void setGlobalMsgObj(ArrayList<FlickrPhoto> str) {
        globalMsgObj = str;
      }

      public boolean checkForNewMsgsOnFirstOpen(){

        // First time opening this app?
        SharedPreferences prefs = getSharedPreferences("SharedPrefs", MODE_PRIVATE);
        boolean firstOpen = prefs.getBoolean("FirstTime", true);
        if (firstOpen == true) {

          // Check server for unreceived messages
          ParseQuery<ParseObject> query = ParseQuery.getQuery("SendMsg");
          query.whereEqualTo("keyOtherUsrPhone", getGlobalLocalUsrPhone());
          query.addAscendingOrder("keyMsgDateTime");
          query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> m, ParseException e) {
              if (e != null) { // If errors, print and break
                return;
              }
              if (m.size() > 0) { // Records received, add to db
                DatabaseHandler dbNew = new DatabaseHandler(getGlobalMainContext());
                for (int i = 0; i < m.size(); i++) {
                  String[] sa = getPhoneAndUID(m.get(i).getString("keyLocalUsrPhone"));
                  String dn = sa[0];
                  int other_usr_id = 0;
                  if (sa[1] != null){
                    other_usr_id = Integer.valueOf(sa[1]);
                  }
                  MsgDb mdb = new MsgDb(0, m.get(i).getString("keyMsgTxt"), m.get(i).getString("keyImgUri"), other_usr_id,
                  m.get(i).getString("keyLocalUsrPhone"), dn, m.get(i).getString("keyOtherUsrPhone"),
                  // Note, switched keyOtherUsrPhone and keyLocalUsrPhone mappings due to design of app
                  m.get(i).getLong("keyMsgDateTime"), m.get(i).getString("keySenderPhone"), m.get(i).getInt("keyMsgStatus"));

                  // Insert new record in local db
                  dbNew.addMessage(mdb);

                  // Delete record on server
                  ParseObject.createWithoutData("SendMsg", m.get(i).getObjectId()).deleteEventually();

                  // Route app UI response appropriately
                  routeNotification(mdb);
                }
                dbNew.close();
              }
            }
          });

          // Update Shared Prefs
          SharedPreferences.Editor editor = getGlobalMainContext().getSharedPreferences("SharedPrefs", Context.MODE_PRIVATE)
          .edit();
          editor.putBoolean("FirstTime",false);
          editor.commit();
          return true;
        }

        return false;
      }

      @Override
      public void onCreate() {

        super.onCreate();
        mContext = getApplicationContext();

        // Initialize Parse
        Parse.initialize(this, "NvCuaRP4Sxmmc28zwRERrqJ5GtgtqRun74ApwdVr",
        "2CFg5BWquyxdUnJxIkp0sKk6BVwetWbWUws3SfVp");

        ParsePush.subscribeInBackground("", new SaveCallback() {
          @Override
          public void done(ParseException e) {
            if (e == null) {
              phoneCheck();
            } else {
              Log.e("com.parse.push", "Failed to subscribe on server", e);
            }
          }
        });

        // Create global UIL configuration and initialize ImageLoader with this
        // configuration
        DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
        .cacheInMemory(true).cacheOnDisk(true).build();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
        getApplicationContext()).defaultDisplayImageOptions(
        defaultOptions).build();
        ImageLoader.getInstance().init(config);
        registerActivityLifecycleCallbacks(new MyLifecycleHandler());
      }

      protected void phoneCheck() {

        // Check if phone number already on server
        // See if Parse object id is already stored, if not, get it and store it
        ParseInstallation curInstall = ParseInstallation.getCurrentInstallation();

        // Store local user's parse object id into global variable
        setGlobalParseObject(curInstall);

        // Check if phone already exists in object on Parse, if not, add it
        ParseQuery<ParseInstallation> query = ParseInstallation.getQuery();
        query.whereEqualTo("objectId", curInstall.getObjectId());
        query.whereExists("UserPhone");
        query.findInBackground(new FindCallback<ParseInstallation>() {
          public void done(List<ParseInstallation> l, ParseException e) {
            if (e == null) {

              // If phone number not currently on the server
              if (l.size() == 0) {

                // get phone number from the phone
                String phone = getGlobalLocalUsrPhone();

                // Update installation record on server with phone
                updatePhoneOnServer(phone);
              }
              Log.w("RECORDS", "Retrieved " + l.size() + " phone#'s");
            } else {
              Log.w("RECORDS", "Error: " + e.getMessage());
            }
          }
        });
      }

      protected void updatePhoneOnServer(String mPhoneNumber) {
        ParseInstallation curInstall = ParseInstallation
        .getCurrentInstallation();
        ParseQuery<ParseInstallation> query = ParseInstallation.getQuery();
        final String pn = mPhoneNumber;

        // Retrieve the object by id
        query.getInBackground(curInstall.getObjectId(),
        new GetCallback<ParseInstallation>() {
          public void done(ParseInstallation o, ParseException e) {
            if (e == null) {
              o.put("UserPhone", pn);
              o.saveInBackground();
            }
          }
        });
      }

      public static Context getContext() {
        return mContext;
      }

      public String formatPhoneNum(String s) {

        String phone = s;

        // Format phone number
        phone = phone.replace(" ", "");
        phone = phone.replace("(", "");
        phone = phone.replace(")", "");
        phone = phone.replace("-", "");
        phone = phone.replace(".", "");
        phone = phone.replace("_", "");
        if (Integer.parseInt(phone.substring(0, 1)) == 1) {
          phone = phone.substring(1);
        }
        return phone;
      }

      public Context globalMainContext;

      public void setGlobalMainContext(Context c) {
        globalMainContext = c;
      }

      public Context getGlobalMainContext() {
        return globalMainContext;
      }

      public void routeNotification(MsgDb m) {
        if (m != null) {

          // If in same convo then refresh screen - Confirm user is in the
          // same convo before updating the screen (if in a different convo,
          // do nothing)
          if (globalCurrentScreen == "ConvoStream"
          && m._other_usr_phone.equals(getGlobalConvoOUP())) {
            Activity ma = (Activity) globalMainContext;
            FragConvoStream fcs = (FragConvoStream) ma.getFragmentManager()
            .findFragmentByTag("msg_convo");

            // Wait for notification to happen
            try {
              Thread.sleep(1500, 0);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            fcs.updateConvoMsgsToRead(m._other_usr_phone);
            fcs.reloadData(m, globalMainContext);
          }

          // If on message stream screen
          if (globalCurrentScreen == "MsgStream") {
            Activity ma = (Activity) globalMainContext;
            FragMsgStream fms = (FragMsgStream) ma.getFragmentManager()
            .findFragmentByTag("msg_stream");
            if (fms != null)
            fms.reloadData();
          }
        }
      }

      public String[] getPhoneAndUID(String ou_phone) {

        // lookup other_usr_id and toDisplayName in contacts
        String[] projection = new String[]{
          ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
          ContactsContract.Contacts._ID};
          Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(ou_phone));
          ContentResolver contentResolver = mContext.getContentResolver();
          Cursor people = contentResolver.query(uri, projection, null, null, null);
          int indexName = people
          .getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
          int indexNumber = people
          .getColumnIndex(ContactsContract.Contacts._ID);
          String[] sa = {null, null};
          if (people.moveToFirst()) {
            sa[0] = people.getString(indexName); // Display Name
            sa[1] = people.getString(indexNumber); // Other User Id
          } else {
            sa[0] = PhoneNumberUtils.formatNumber(ou_phone);
          }
          people.close();
          return sa;
        }

        public void sendOnboardingAlert(final String ckBoxName, String title, CharSequence msg ){
          final SharedPreferences prefs = getSharedPreferences("SharedPrefs", MODE_PRIVATE);
          int ckbox = prefs.getInt(ckBoxName, 0);

          // Check Shared Prefs to see if checkbox was previously checked
          // If not, set up alert dialogue
          if (ckbox != 1) {
            AlertDialog.Builder adb = new AlertDialog.Builder(getGlobalMainContext());
            LayoutInflater adbInflater = LayoutInflater.from(getGlobalMainContext());
            View eulaLayout = adbInflater.inflate(R.layout.checkbox, null);
            final CheckBox dontShowAgain = (CheckBox) eulaLayout.findViewById(R.id.skip);
            adb.setView(eulaLayout);
            adb.setTitle(title);
            adb.setMessage(msg);
            adb.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
                int checkBoxResult = 0;
                if (dontShowAgain.isChecked())
                checkBoxResult = 1;
                //SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(ckBoxName, checkBoxResult);
                editor.commit();
                return;
              }
            });
            adb.show();
          }
        }
      }
