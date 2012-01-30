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
import java.io.BufferedReader;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.text.ParseException;
import android.text.method.ScrollingMovementMethod;
import android.app.ProgressDialog;
import android.app.Dialog;
import android.content.DialogInterface;
//import android.widget.ImageButton;
import android.widget.Button;

public class Playing extends Activity implements OnClickListener, 
        OnPlaybackStartedListener, OnPlaybackStoppedListener {

	public final static String ACTION_NOW_PLAYING_CHANGED = "org.chirpradio.mobile.TRACK_CHANGED";
    private final static int PROGRESS_DIALOG = 1;
	
    private PlaybackService playbackService;
    private ServiceConnection serviceConnection;
	private Boolean serviceIsBound;
	private AudioManager audioManager;
	private TextView nowPlayingTextView;
    private TextView recentlyPlayedTextView;
    //private ImageButton playButton;
    private Button playButton;
    private LinkedList<Track> playlist;
    private ProgressDialog progress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playing);
        
        //doBindService();
        //setupNotification();
        
		nowPlayingTextView = (TextView) findViewById(R.id.now_playing);	
		recentlyPlayedTextView = (TextView) findViewById(R.id.previous);
        recentlyPlayedTextView.setMovementMethod(new ScrollingMovementMethod());
        //playButton = (ImageButton)findViewById(R.id.play_button);
        playButton = (Button)findViewById(R.id.play_button);
        playButton.setOnClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        Debug.log(this, "onStart called - binding");
        doBindService();
        registerReceiver(nowPlayingReceiver, new IntentFilter("CHIRP"));
    }

    @Override
    public void onStop() {
        super.onStop();
        Debug.log(this, "onStop called - unbinding");
        if(playbackService != null) {
            if(!playbackService.isPlaying()) {
                Debug.log(this, "UI stopped while not playing - can shut down svc");
                //getApplicationContext().stopService(playbackService);
            }
        }

        // wtf.  unbinding here throws an exception saying the service is already
        // unbound.  so... i guess i'll be leaking serviceconnections
        //doUnbindService();
        unregisterReceiver(nowPlayingReceiver);
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
                    if(playbackService != null) {
                        if(playbackService.isPlaying()) {
                            Debug.log(this, "changing button to stop icon");
                            //playButton.setImageResource(R.drawable.stop);
                            playButton.setText("Stop");
                            //playButton.setEnabled(false);
                            //stopButton.setEnabled(true);
                        } else {
                            Debug.log(this, "changing button to play icon");
                            //playButton.setImageResource(R.drawable.play);
                            playButton.setText("Play");
                            //playButton.setEnabled(true);
                            //stopButton.setEnabled(false);
                        }
                    } else {
                        Debug.log(this, "playbackService is null in onServiceConnected");
                    }
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
            if(playbackService.isPlaying()) {
                // stop
                playbackService.stop();
                //playButton.setImageResource(R.drawable.play);
                playButton.setText("Play");
            } else {
                // play 
                //playButton.setImageResource(R.drawable.stop);
                playButton.setText("Stop");
                showDialog(PROGRESS_DIALOG);
                playbackService.start();
            }
			break;
		}
	}

	public void updateCurrentlyPlaying(LinkedList<Track> tracks) {
		
        Track currentTrack = tracks.get(0);

		final String nowPlayingContent = 
            "<font color=#FCFC77>NOW PLAYING</font> &#183;" + " <b>ON-AIR:</b> " +
            currentTrack.getDj() + "<br><br><hr>" + "<b>" + currentTrack.getArtist() + "</b>" +
            " - " + currentTrack.getTrack() + " <i>from " + currentTrack.getRelease() + " (" +
            currentTrack.getLabel() + ")" + "</i>";

	    		nowPlayingTextView.setText(Html.fromHtml(nowPlayingContent));	 
		
		String recentlyPlayedContent = "<font color=#FCFC77>RECENTLY PLAYED</font>" + "<br>";

		for (int i = 1; i < tracks.size(); i++) {
			Track recentTrack = tracks.get(i);
            recentlyPlayedContent += "<b>" + recentTrack.getArtist() + "</b>" + 
                " - " + recentTrack.getTrack() + " <i>from " + 
                recentTrack.getRelease() + " (" + recentTrack.getLabel() + ")" + "</i>";				
			if (i < tracks.size()-1) {
				recentlyPlayedContent += "<br><br><hr>";
			}						
    	}
        recentlyPlayedTextView.setText(Html.fromHtml(recentlyPlayedContent));	  	    	
	}

    /* 
     *  this broadcast receiver gets data from the service to update the ui
     */
	private BroadcastReceiver nowPlayingReceiver = new BroadcastReceiver () {
	    @Override
        public void onReceive(Context arg0, Intent intent) {
            String json = intent.getStringExtra("playlist");
            playlist = new LinkedList<Track>();
            try {
                JSONObject j = new JSONObject(json);
                Track t = new Track(new JSONObject(json).getJSONObject("now_playing"));
                playlist.add(t);
                JSONArray previous = new JSONObject(json).getJSONArray("recently_played");
    
                for(int i = 0 ; i < 5 ; i++) {
                    t = new Track(previous.getJSONObject(i));
                    playlist.add(t);
                }
                /*t = new Track(previous.getJSONObject(1));
                playlist.add(t);
                t = new Track(previous.getJSONObject(2));
                playlist.add(t);*/
                updateCurrentlyPlaying(playlist);
            } catch (Exception e) {
                Debug.log(this, "Error parsing now_playing: " + e.toString());
            }

        };
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
                //cancelNotification();
				findViewById(R.id.play_button).setEnabled(true);
				//findViewById(R.id.stop_button).setEnabled(false);
				Debug.log(this, "playback stopped");
		    }
		});
	}

	@Override
	public void onPlaybackStarted() {
                progress.dismiss();
		runOnUiThread(new Runnable() {
		    public void run() {
                //Track t = playlist.get(0);
                //setNotification("CHIRP Radio", t.getArtist() + " - " + t.getTrack());
                //findViewById(R.id.stop_button).setEnabled(true);
				Debug.log(this, "playback started");
		    }
		});
	}    

    protected Dialog onCreateDialog(int id) {
        switch(id) {
            case PROGRESS_DIALOG:
            Debug.log(this, "creating dialog");
                progress = new ProgressDialog(this);
                progress.setMessage("Please Wait");
                progress.setTitle("Loading");
                progress.setIndeterminate(true);
                progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        Debug.log(this,"cancelling dialog");
                        // ok
                    }
                });
                return progress;
            default:
                return null;
        }
    }
    
}
