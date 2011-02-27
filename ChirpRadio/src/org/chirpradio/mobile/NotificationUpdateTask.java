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

import android.util.Log;
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
public class NotificationUpdateTask extends AsyncTask<Context, Void, Track> {

	private static final String LOG_TAG = NotificationUpdateTask.class.toString();
	private static final int NOTIFICATION_ID = 1;
	private Context context;
	
    protected Track doInBackground(Context... contexts) {
		android.os.Debug.waitForDebugger();
    	this.context = contexts[0];
    	Track track = Track.getCurrentTrack();
		return track;
    }

    protected void onPostExecute(Track track) {
	    int icon = R.drawable.icon;	
	    long when = System.currentTimeMillis();
	    
		try {
			if (track != null) {
				String notificationString = track.getArtist() + " - " + track.getTrack() + " from " + '"'+ track.getRelease() + '"';
	
			    CharSequence title = context.getString(R.string.app_name) + " (DJ" + " " + track.getDj()+ ")";
			    
			    /* Intent for the Playing UI */
			    //IntentFilter intentFilter = new IntentFilter(Playing.ACTION_NOW_PLAYING_CHANGED);
			    //Intent intent = new Intent(context, Playing.class);	
			    Intent intent = new Intent(Playing.ACTION_NOW_PLAYING_CHANGED);
			    intent.putExtra("track", track);
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