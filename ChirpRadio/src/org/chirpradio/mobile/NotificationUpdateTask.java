package org.chirpradio.mobile;

import android.util.Log;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import android.text.Html;

import android.os.AsyncTask;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;

public class NotificationUpdateTask extends AsyncTask<Context, Void, JSONObject> {

	private static final int NOTIFICATION_ID = 1;
	private Context context;
	
    protected JSONObject doInBackground(Context... contexts) {
    	Context context = contexts[0];
    	this.context = context;
        HttpGet get = new HttpGet("http://chirpradio.org/json");
        DefaultHttpClient client = new DefaultHttpClient();
        try {
			HttpResponse response = client.execute(get);
			String responseBody = EntityUtils.toString(response.getEntity());
			
			// JSON response contains invalid 'comments', so strip them out
			responseBody = responseBody.replaceFirst("<!-- cached copy -->", "");
			responseBody = responseBody.replaceFirst("<!-- end cached copy -->", "");
			
			// Decode HTML entities
			responseBody = Html.fromHtml(responseBody).toString();
			
			try {
				JSONObject object = (JSONObject) new JSONTokener(responseBody).nextValue();
				Log.i("artist", object.getString("artist"));
				Log.i("song-title", object.getString("song-title"));
				Log.i("album", object.getString("album"));
				return object;
			} catch (JSONException e) {
				// error parsing JSON
				e.printStackTrace();
			}
			
		} catch (ClientProtocolException e) {
			// in case of a problem or the connection was aborted
			e.printStackTrace();
		} catch (IOException e) {
			// in case of an http protocol error
			e.printStackTrace();
		}
		return new JSONObject();
    }

    protected void onPostExecute(JSONObject data) {
	    int icon = R.drawable.icon;
	    long when = System.currentTimeMillis();
	    
	    String artist;
		try {
			artist = data.getString("artist");
		    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		    
		    Notification notification = new Notification(icon, artist, when);
		    notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
		    
		    Context c = context.getApplicationContext();
		    CharSequence title = context.getString(R.string.app_name);
		    Intent notificationIntent;
		    notificationIntent = new Intent(c, MainMenu.class);
		    notificationIntent.setAction(Intent.ACTION_VIEW);
		    notificationIntent.addCategory(Intent.CATEGORY_DEFAULT);
		    notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		    PendingIntent contentIntent = PendingIntent.getActivity(c, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		    notification.setLatestEventInfo(c, title, artist, contentIntent);
		    notificationManager.notify(NOTIFICATION_ID, notification);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }
	
}
