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

	private static final String LOG_TAG = NotificationUpdateTask.class.toString();
	private static final int NOTIFICATION_ID = 1;
	
	private Context context;
	
    protected JSONObject doInBackground(Context... contexts) {
    	this.context = contexts[0];
    	
        HttpGet get = new HttpGet("http://chirpradio.org/json");
        DefaultHttpClient client = new DefaultHttpClient();
        
        try {
			HttpResponse response = client.execute(get);
			String responseBody = EntityUtils.toString(response.getEntity());
			
			// JSON response contains invalid 'comments', so strip them out
			responseBody = responseBody.replaceFirst("<!-- cached copy -->", "");
			responseBody = responseBody.replaceFirst("<!-- end cached copy -->", "");
			responseBody = responseBody.replaceFirst("<!-- fresh copy -->", "");
			responseBody = responseBody.replaceFirst("<!-- end fresh copy -->", "");
			
			// Decode HTML entities
			responseBody = Html.fromHtml(responseBody).toString();
			
			try {
				return (JSONObject) new JSONTokener(responseBody).nextValue();
			} catch (JSONException e) {
		        Log.e(LOG_TAG, "", e);
			}
			
		} catch (ClientProtocolException e) {
			// a problem connecting or the connection was aborted
	        Log.e(LOG_TAG, "", e);
		} catch (IOException e) {
			// an http protocol error
	        Log.e(LOG_TAG, "", e);
		}
		return new JSONObject();
    }

    protected void onPostExecute(JSONObject data) {
	    int icon = R.drawable.icon;
	    long when = System.currentTimeMillis();
	    
		try {
			String artist = data.getString("artist");

		    CharSequence title = context.getString(R.string.app_name);
		    Intent notificationIntent = new Intent(context, MainMenu.class);
		    notificationIntent.setAction(Intent.ACTION_VIEW);
		    notificationIntent.addCategory(Intent.CATEGORY_DEFAULT);
		    notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		    
		    PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		    
		    Notification notification = new Notification(icon, artist, when);
		    notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
		    notification.setLatestEventInfo(context, title, artist, contentIntent);
		    
		    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		    notificationManager.notify(NOTIFICATION_ID, notification);
		} catch (JSONException e) {
	        Log.e(LOG_TAG, "", e);
		}

    }
}