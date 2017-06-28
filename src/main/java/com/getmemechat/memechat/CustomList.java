package com.getmemechat.memechat;
import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.nostra13.universalimageloader.core.ImageLoader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CustomList extends ArrayAdapter<MsgDb> {
	private LayoutInflater inflater;
	List<MsgDb> messages = null;

	MyApplication mApp = (MyApplication) MyApplication.getContext();

	public CustomList(Context context, int textViewResourceId,
			List<MsgDb> messageList) {
		super(context, textViewResourceId, messageList);
		inflater = ((Activity) context).getLayoutInflater();
		messages = messageList;
	}

	private static class ViewHolder {
		TextView textView;
		TextView textView2;
        TextView textView3;
		ImageView imageView;
		public String imageURL;
		public String name;
		public String date;
        public String msg;
	}

	// @Override
	public View getView(int position, View convertView, ViewGroup parent) {

		ViewHolder viewHolder = null;

		// check if there is already a holder set up
		if (convertView == null) {

			// inflate the layout
			convertView = inflater.inflate(R.layout.list_single, parent, false);

			// assign the views in the view holder
			viewHolder = new ViewHolder();
			viewHolder.textView = (TextView) convertView.findViewById(R.id.txt);

			// bold list items that haven't been opened yet
			String p = messages.get(position)._sender_phone;
			if (messages.get(position)._msg_status == 7 || messages.get(position)._sender_phone.trim().equals(mApp.getGlobalLocalUsrPhone().trim())){
				viewHolder.textView.setTypeface(null, Typeface.NORMAL);
			} else {
				viewHolder.textView.setTypeface(null, Typeface.BOLD);
			}

			viewHolder.imageView = (ImageView) convertView
					.findViewById(R.id.img);

			viewHolder.textView2 = (TextView) convertView.findViewById(R.id.txt2);
            viewHolder.textView3 = (TextView) convertView.findViewById(R.id.txt3);

			// store the view holder with the view.
			convertView.setTag(viewHolder);
		}

		// store the latest data within view holder
		viewHolder = (ViewHolder) convertView.getTag();
		viewHolder.imageURL = messages.get(position)._img_uri;
		viewHolder.name = messages.get(position)._other_usr_name;
		String dateString = new SimpleDateFormat("EEE, LLL dd hh:mm a", Locale.US)
		.format(new Date(messages.get(position)._msg_date_time));
		viewHolder.date = dateString;
        viewHolder.msg = messages.get(position).getMsg();

        // Check msg length to ensure it will fit on the screen
        if (viewHolder.msg.length() > 83)
            viewHolder.msg = viewHolder.msg.substring(0,83) + "...";

		// set values in the view holder views
		ImageLoader.getInstance().displayImage(viewHolder.imageURL,
				viewHolder.imageView);
		viewHolder.textView.setText(viewHolder.name);
		viewHolder.textView2.setText(viewHolder.date);
        viewHolder.textView3.setText(viewHolder.msg);

		// bold list items that haven't been opened yet
		String p = messages.get(position)._sender_phone;
		if (messages.get(position)._msg_status == 7 || messages.get(position)._sender_phone.trim().equals(mApp.getGlobalLocalUsrPhone().trim())){
			viewHolder.textView.setTypeface(null, Typeface.NORMAL);
		} else {
			viewHolder.textView.setTypeface(null, Typeface.BOLD);
		}
		
		return convertView;
	}
}
