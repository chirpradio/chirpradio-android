// Copyright 2011 The Chicago Independent Radio Project 
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.chirpradio.mobile;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;

import android.util.Log;
import android.widget.TextView;
import android.os.AsyncTask;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;

import org.chirpradio.mobile.Track;

/** NotificationUpdateTask encapsulates the background job that runs to fetch 
* things like track information from the CHIRP server. It also includes callbacks
* to do things like update the UI from the info received from the background job.
*
* @author Jim Benton, Chirag Patel
*/
public class NotificationUpdateTask extends AsyncTask<Context, Void, Hashtable<String, Serializable>> {

	private static final String LOG_TAG = NotificationUpdateTask.class.toString();
	private static final int NOTIFICATION_ID = 1;
	private Context context;
	
    protected Hashtable<String, Serializable> doInBackground(Context... contexts) {
		android.os.Debug.waitForDebugger();
    	this.context = contexts[0];
    	Hashtable<String, Serializable> current_playlist = Track.getCurrentPlaylist();
		return current_playlist;
    }

    protected void onPostExecute(Hashtable<String, Serializable> current_playlist) {
	    int icon = R.drawable.icon;	
	    long when = System.currentTimeMillis();
	    
		try {
			if ((Integer)current_playlist.get("attempts") == 1) {
				Track now_playing = (Track)current_playlist.get("now_playing");
				
				String notificationString = now_playing.getArtist() + " - " + now_playing.getTrack() + " from " + '"'+ now_playing.getRelease() + '"';
				
				String title_string = context.getString(R.string.app_name) + " (";
				String dj_string = now_playing.getDj(); 			

				if (!(dj_string.toLowerCase().charAt(0) == 'd' && dj_string.toLowerCase().charAt(0) == 'j') && !dj_string.equals("Error")) {
					title_string += "DJ ";
				}
				
				title_string += dj_string+ ")";
				CharSequence title = title_string;
			    /* Intent for the Playing UI */
			    //IntentFilter intentFilter = new IntentFilter(Playing.ACTION_NOW_PLAYING_CHANGED);
			    //Intent intent = new Intent(context, Playing.class);	
			    Intent intent = new Intent(Playing.ACTION_NOW_PLAYING_CHANGED);
			    intent.putExtra("now_playing", now_playing);
				ArrayList<Track> recently_played = (ArrayList<Track>)current_playlist.get("recently_played");
				for (int i = 0; i < 3; ++i) {
				    intent.putExtra("recently_played"+String.valueOf(i), recently_played.get(i));			
		    	}

			    context.sendBroadcast(intent);
			   	
			    /* Intent for the Notification area */
			    Intent notificationIntent = new Intent(context, Playing.class);
			    notificationIntent.setAction(Intent.ACTION_VIEW);
			    notificationIntent.addCategory(Intent.CATEGORY_DEFAULT);
			    notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			    
			    PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

                // TODO: Abstract this out into Track.send_notification_to_manager		    
			    Notification notification = new Notification(icon, notificationString, when);
			    notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
			    notification.setLatestEventInfo(context, title, notificationString, contentIntent);
			    
			    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			    notificationManager.notify(NOTIFICATION_ID, notification);
			}
		} catch (Exception e) {
	        Log.e(LOG_TAG, "", e);
		}

    }
}