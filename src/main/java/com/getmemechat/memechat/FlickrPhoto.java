package com.getmemechat.memechat;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FlickrPhoto extends Object{
	String id;
	String owner;
	String secret;
	String server;
	String farm;
	String title;
	Boolean isPublic;
	Boolean isFriend;
	Boolean isFamily;

	public FlickrPhoto(JSONObject jsonPhoto) throws JSONException{
		this.id=(String) jsonPhoto.optString("id");
		this.secret=(String) jsonPhoto.optString("secret");
		this.owner=(String) jsonPhoto.optString("owner");
		this.server=(String) jsonPhoto.optString("server");
		this.farm=(String) jsonPhoto.optString("farm");
		this.title=(String) jsonPhoto.optString("title");
		this.isPublic=(Boolean) jsonPhoto.optBoolean("ispublic");
		this.isFriend=(Boolean) jsonPhoto.optBoolean("isfriend");
		this.isFamily=(Boolean) jsonPhoto.optBoolean("isfamily");
	}

	public String getPhotoUrl(Boolean big){
	    String opt = "n";
	    if (big)
	    	opt ="c";
		String photoUri =
		"http://farm"+ this.farm + ".staticflickr.com/" + this.server +
		"/" + this.id+"_" + this.secret + "_" + opt + ".jpg";
		return photoUri;
	}

	public static ArrayList <FlickrPhoto> makePhotoList (String photoData ) throws JSONException, NullPointerException {
		ArrayList <FlickrPhoto> flickrPhotos = new ArrayList<FlickrPhoto>();
		JSONObject data  = new JSONObject(photoData);
		JSONObject photos = data.optJSONObject("photos");
		JSONArray photoArray = photos.optJSONArray("photo");
		for(int i = 0; i < photoArray.length(); i++) {
			JSONObject photo=	(JSONObject) photoArray.get(i);
			FlickrPhoto currentPhoto = new FlickrPhoto (photo);
			flickrPhotos.add(currentPhoto);
		}
		return flickrPhotos;
	}
}
