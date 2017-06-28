package com.getmemechat.memechat;
import java.util.ArrayList;
import com.nostra13.universalimageloader.core.ImageLoader;
import android.app.Activity;
import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class ImageAdapter extends PagerAdapter {

	private Activity _activity;
	private ArrayList<FlickrPhoto> _imagePaths;
	private LayoutInflater inflater;

	// constructor
	public ImageAdapter(Activity activity, ArrayList<FlickrPhoto> imagePaths) {
		this._activity = activity;
		this._imagePaths = imagePaths;
	}

	@Override
	public int getCount() {
		return this._imagePaths.size();
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == ((RelativeLayout) object);
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		ImageView imgDisplay;

		inflater = (LayoutInflater) _activity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View viewLayout = inflater.inflate(R.layout.layout_fullscreen_image,
				container, false);

		imgDisplay = (ImageView) viewLayout.findViewById(R.id.imgDisplay);

		FlickrPhoto currentPhoto = _imagePaths.get(position);
		String url = currentPhoto.getPhotoUrl(false);
		ImageLoader.getInstance().displayImage(url, imgDisplay);
		((ViewPager) container).addView(viewLayout);

		return viewLayout;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		((ViewPager) container).removeView((RelativeLayout) object);
	}
}
