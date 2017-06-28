package com.getmemechat.memechat;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import java.util.List;

public class FragMsgStream extends Fragment {

	DataPassListener mCallback;

	public interface DataPassListener {
		public void passConvo(String oup, String s);
	}

	MyApplication mApp = (MyApplication) MyApplication.getContext();
	ListView listView;
	public static final String DB_CREATE_ACTION = "brCreateTableComplete";

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Make sure that container activity implement the callback interface
		try {
			mCallback = (DataPassListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
			+ " must implement DataPassListener");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.frag_msg_stream, container, false);
		listView = (ListView) v.findViewById(R.id.list);
		reloadData();
		return v;
	}

	@Override
	public void onResume() {
		super.onResume();

		if (mApp.getGlobalNotificationCounter() > 0) {
			reloadData();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	public void reloadData () {

		// Open db, run query and store in array of MsgDb objects
		DatabaseHandler db = new DatabaseHandler((Activity) mApp.globalMainContext);
		final List<MsgDb> messages = db.getLatestMessages();
		db.close();

		// Pass array of MsgDb objects to Adapter
		CustomList imageAdapter = new CustomList(mApp.globalMainContext,
		R.layout.list_single, messages);
		listView.setAdapter(imageAdapter);
		imageAdapter.notifyDataSetChanged();
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
			int position, long id) {

				// Get other user id of this message and pass to Main Activity
				mCallback.passConvo(messages.get(position).getOtherUserPhone(), messages.get(position).getOtherUserName());
			}
		});
	}
}
