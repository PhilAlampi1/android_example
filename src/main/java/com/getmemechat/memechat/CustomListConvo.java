package com.getmemechat.memechat;
import com.nostra13.universalimageloader.core.ImageLoader;
import java.util.List;
import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class CustomListConvo extends ArrayAdapter<MsgDb> {
	private LayoutInflater inflater;
	List<MsgDb> messages = null;
	MyApplication mApp = (MyApplication) MyApplication.getContext();

	public CustomListConvo(Context context, int textViewResourceId,
			List<MsgDb> messageList) {
		super(context, textViewResourceId, messageList);
		inflater = ((Activity) context).getLayoutInflater();
		messages = messageList;
	}

	private static class ViewHolder {
		TextView textView;
		// TextView textView2;
		ImageView imageView;
		public String imageURL;
		public String msg;
		public Long dt;
	}

	// @Override
	public View getView(int position, View convertView, ViewGroup parent) {

		ViewHolder viewHolder = null;

		// check if there is already a holder set up
		if (convertView == null) {

			// inflate the layout
			convertView = inflater.inflate(R.layout.convo_list_single, parent, false);

			// assign the views in the view holder
			viewHolder = new ViewHolder();
			viewHolder.textView = (TextView) convertView.findViewById(R.id.TextViewConvo);

			// determine if text should be right (local user) aligned
			if (messages.get(position)._sender_phone.equalsIgnoreCase(messages.get(position)._local_usr_phone)){
				viewHolder.textView.setGravity(Gravity.END);
			} else {
				viewHolder.textView.setGravity(Gravity.START);
			}
			viewHolder.imageView = (ImageView) convertView
					.findViewById(R.id.imgConvo);

			//resize imageview height to = screen width (global variable)
			LayoutParams params = viewHolder.imageView.getLayoutParams();
			params.height = mApp.getWidth();
			viewHolder.imageView.setLayoutParams(params);

			// store the view holder with the view.
			convertView.setTag(viewHolder);
		}

		// store the latest data within view holder
		viewHolder = (ViewHolder) convertView.getTag();
		viewHolder.imageURL = messages.get(position)._img_uri;
		viewHolder.msg = messages.get(position)._msg_txt;
		viewHolder.dt = messages.get(position)._msg_date_time;

		// set values in the view holder views
		ImageLoader.getInstance().displayImage(viewHolder.imageURL,
				viewHolder.imageView);
		viewHolder.textView.setText(viewHolder.msg);

		// Align text to the right if from the local user
		if (messages.get(position)._sender_phone.equalsIgnoreCase(messages.get(position)._local_usr_phone)){
			viewHolder.textView.setGravity(Gravity.END);
			//Log.w("IF CONDITION MET", "GRAVITY SET");
			//Log.w("BOTTOM IN IF DATA", "Sender: " + messages.get(position)._sender_phone + ", Local: " + messages.get(position)._local_usr_phone);
		} else {
			viewHolder.textView.setGravity(Gravity.START);
		}

		return convertView;
	}
}
