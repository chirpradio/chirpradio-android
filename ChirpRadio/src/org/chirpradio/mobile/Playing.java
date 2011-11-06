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

import java.util.ArrayList;
import java.util.LinkedList;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.app.NotificationManager;
import android.app.Notification;
import android.os.AsyncTask;
import java.io.BufferedReader;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.text.ParseException;
import android.os.Handler;

public class Playing extends Activity implements OnClickListener, 
        OnPlaybackStartedListener, OnPlaybackStoppedListener {

	public final static String ACTION_NOW_PLAYING_CHANGED = "org.chirpradio.mobile.TRACK_CHANGED";
	
    private PlaybackService playbackService;
    private ServiceConnection serviceConnection;
	private Boolean serviceIsBound;
	private AudioManager audioManager;
	private TextView nowPlayingTextView;
    private TextView recentlyPlayedTextView;
    private View playButton;
    private View stopButton;
    private TextView playStatus;
    private LinkedList<Track> playlist;
    // for updating the playlist view
    private Handler handler;
    private Boolean updatePlaylist;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playing);
        
        //doBindService();
        //setupNotification();
        findViewById(R.id.stop_button).setEnabled(false);
        
		nowPlayingTextView = (TextView) findViewById(R.id.now_playing);	
		recentlyPlayedTextView = (TextView) findViewById(R.id.previous);
        playStatus = (TextView)findViewById(R.id.play_status);
        playButton = findViewById(R.id.play_button);
        playButton.setOnClickListener(this);
        stopButton = findViewById(R.id.stop_button);
        stopButton.setOnClickListener(this);

        updatePlaylist = true;
        handler = new Handler();
        handler.post(mUpdateTask);
    }

    // handles updating the get playlist task
    private Runnable mUpdateTask = new Runnable() {
        public void run() {
            if(updatePlaylist) {
                Debug.log(this, "updating playlist from timer");
                new GetPlaylistTask().execute();
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        Debug.log(this, "onStart called - binding");
        doBindService();
        updatePlaylist = true;
        registerReceiver(headphoneReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
    }

    @Override
    public void onStop() {
        super.onStop();
        Debug.log(this, "onStop called - unbinding");
        updatePlaylist = false;
        // wtf.  unbinding here throws an exception saying the service is already
        // unbound.  so... i guess i'll be leaking serviceconnections
        //doUnbindService();
    }
    
    /*private void setupNotification() {
        try {
            Long firstTime = SystemClock.elapsedRealtime();
            
            // create an intent that will call NotificationUpdateReceiver
            Intent intent  = new Intent(this, NotificationUpdateReceiver.class);
            
            // create the event if it does not exist
            PendingIntent sender = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            
            // call the receiver every 10 seconds
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            am.setRepeating(AlarmManager.ELAPSED_REALTIME, firstTime, 10000, sender);

            
            
       } catch (Exception e) {
            Debug.log(this, e.toString());
       }
    }*/

    private static final int CHIRP_ID = 1019;
    private void setNotification(String title, String message) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
        CharSequence tickerText = message;
        long when = System.currentTimeMillis();

        int icon = R.drawable.icon;
        Notification notification = new Notification(icon, tickerText, when);
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        Context context = getApplicationContext();
        CharSequence contentTitle = title;
        CharSequence contentText = message;
        Intent notificationIntent = new Intent(this, Playing.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

        mNotificationManager.notify(CHIRP_ID, notification);
    }

    private void cancelNotification() {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
        mNotificationManager.cancel(CHIRP_ID);
    }

    private void setupPlaybackListeners() {
        playbackService.setOnPlaybackStartedListener(this);
        playbackService.setOnPlaybackStoppedListener(this);	
    }
    
    void doBindService() {
    	
    	Intent serviceIntent = new Intent(this, PlaybackService.class);
    	serviceConnection = new ServiceConnection() {
	      @Override
	      public void onServiceConnected(ComponentName name, IBinder service) {
	    	  playbackService = ((PlaybackService.PlaybackBinder) service).getService();
	        Debug.log(this, "CONNECTED");
	        setupPlaybackListeners();
	      }

	      @Override
	      public void onServiceDisconnected(ComponentName name) {
	        Debug.log(this, "DISCONNECT");
	       // playbackService = null;
	      }
	    };
	    getApplicationContext().startService(serviceIntent);
	    getApplicationContext().bindService(serviceIntent, serviceConnection, 0);
       
        serviceIsBound = true;
    }

    void doUnbindService() {
        if (serviceIsBound) {
            // Detach our existing connection.
            Debug.log(this, "UNBINDING SERVICE!!!!!!!!!!!!!!!!!!!!!!!!!!");
            unbindService(serviceConnection);
            serviceIsBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //doUnbindService();
    }

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.play_button:
            playStatus.setText("Bufferring Audio");
            //setNotification("CHIRPRadio.org", "Playing");
			playbackService.start();
			v.setEnabled(false);
            stopButton.setEnabled(true);
			break;
		case R.id.stop_button:
            playStatus.setText("Stopping");
			playbackService.stop();
			v.setEnabled(false);
            playButton.setEnabled(true);
			break;
		}
	}

	public void updateCurrentlyPlaying(LinkedList<Track> tracks) {
		
        Track currentTrack = tracks.get(0);

		final String nowPlayingContent = "<font color=#FCFC77>NOW PLAYING</font> &#183;" + " <b>ON-AIR:</b> " +
										 currentTrack.getDj() + "<br><br><hr>" + "<b>" + currentTrack.getArtist() + "</b>" +
										 " - " + currentTrack.getTrack() + " <i>from " + currentTrack.getRelease() + " (" +
										 currentTrack.getLabel() + ")" + "</i>";
		
	    		nowPlayingTextView.setText(Html.fromHtml(nowPlayingContent));	 
		
		String recentlyPlayedContent = "<font color=#FCFC77>RECENTLY PLAYED</font>" + "<br>";

		for (int i = 1; i < tracks.size(); i++) {
			Track recentTrack = tracks.get(i);
			recentlyPlayedContent += "<b>" + recentTrack.getArtist() + "</b>" + " - " + recentTrack.getTrack() + " <i>from " + 
			recentTrack.getRelease() + " (" + recentTrack.getLabel() + ")" + "</i>";				
			if (i < tracks.size()-1) {
				recentlyPlayedContent += "<br><br><hr>";
			}						
    	}
        recentlyPlayedTextView.setText(Html.fromHtml(recentlyPlayedContent));	  	    	
	}

    // ng: not sure what the point of this broadcast receiver was
	/*private BroadcastReceiver nowPlayingReceiver = new BroadcastReceiver () {
	    @Override
	    public void onReceive(Context arg0, Intent intent) {
	      Debug.log(this, "onReceive called");	
	    }
	};*/

    private BroadcastReceiver headphoneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            Debug.log(this, "headphone broadcast message received");
            if(intent.getAction().equalsIgnoreCase(Intent.ACTION_HEADSET_PLUG)) {
                String data = intent.getDataString();
                Debug.log(this, "data: " + data);
            }
        }
    };
	
    public void onResume() {
        super.onResume();
        //registerReceiver(nowPlayingReceiver, new IntentFilter(ACTION_NOW_PLAYING_CHANGED));
    }

    
    public void onPause() {
        super.onPause();
        //unregisterReceiver(nowPlayingReceiver);
    }

	@Override
	public void onPlaybackStopped() {
		runOnUiThread(new Runnable() {
		    public void run() {
                cancelNotification();
				findViewById(R.id.play_button).setEnabled(true);
				findViewById(R.id.stop_button).setEnabled(false);
                playStatus.setText("Stopped");
				Debug.log(this, "playback stopped");
		    }
		});
	}

	@Override
	public void onPlaybackStarted() {
		runOnUiThread(new Runnable() {
		    public void run() {
                Track t = playlist.get(0);
                setNotification("CHIRP Radio", t.getArtist() + " - " + t.getTrack());
                findViewById(R.id.stop_button).setEnabled(true);
                playStatus.setText("Playback started");
				Debug.log(this, "playback started");
		    }
		});
	}    

    private class GetPlaylistTask extends AsyncTask<Void, Integer, Boolean>
    {
        private boolean result = false;

        protected Boolean doInBackground(Void... no) {
            BufferedReader in = null;
            Boolean result = false;
            try {
                String str = Request.sendRequest();
                result = true;
                Track t = new Track(new JSONObject(str).getJSONObject("now_playing"));
                playlist = new LinkedList<Track>();
                playlist.add(t);

                JSONArray previous = new JSONObject(str).getJSONArray("recently_played");

                t = new Track(previous.getJSONObject(0));
                playlist.add(t);
                t = new Track(previous.getJSONObject(1));
                playlist.add(t);
                t = new Track(previous.getJSONObject(2));
                playlist.add(t);
            } catch (Exception e) {
                Debug.log(this, "Exception getting playlist: " + e.toString());
                result = false;
            } finally {
            }
            return result;
        }

        protected void onPostExecute(Boolean result) {
            Debug.log(this, "Finished getting playlist");
            if(!result) {
                Debug.log(this, "Retrieving playlist failed");
            } else {
                updateCurrentlyPlaying(playlist);
            }
            Debug.log(this, "Next playlist update in 20 seconds");
            Track t = playlist.get(0);
            setNotification("CHIRP Radio", t.getArtist() + " - " + t.getTrack());
            handler.postDelayed(mUpdateTask, 20000);
        }
    }
    
}
