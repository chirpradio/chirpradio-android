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

import org.json.JSONObject;

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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class Playing extends Activity implements OnClickListener, OnSeekBarChangeListener {

	private static final String LOG_TAG = "PlayingActivity";
	public final static String ACTION_NOW_PLAYING_CHANGED = "org.chirpradio.mobile.TRACK_CHANGED";
	
    private PlaybackService playbackService;
    private ServiceConnection serviceConnection;
	private Boolean serviceIsBound;
	private AudioManager audioManager;
	ArrayList<Track> recently_played;
	Track now_playing;
	private TextView nowPlayingText;
	private TextView recentlyPlayedText;
	private TextView playedAt;
	
	private String recently_played_string = new String();
	private String now_playing_string = new String();

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playing);
        
		nowPlayingText = (TextView) findViewById(R.id.now_playing);	
		recentlyPlayedText = (TextView) findViewById(R.id.recently_played);	
		now_playing_string += "<font color=#FCFC77>NOW PLAYING</font> &#183;" + " <b>ON-AIR:</b> " + now_playing.getDj() + "<br><br><hr>" + "Loading...";
		nowPlayingText.setText(Html.fromHtml(now_playing_string));	 
		recently_played_string += "<font color=#FCFC77>RECENTLY PLAYED</font>" + "<br>" + "Loading...";
		recentlyPlayedText.setText(Html.fromHtml(recently_played_string));	  
        //this.registerReceiver(this.nowPlayingReceiver, new IntentFilter(Intent.ACTION_NOW_PLAYING_CHANGED));
        
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        
        doBindService();
        setupUICallbacks();
        setupNotification();
        
    }
    
    private void setupUICallbacks() {
        View playButton = findViewById(R.id.play_button);
        playButton.setOnClickListener(this);
        View stopButton = findViewById(R.id.stop_button);
        stopButton.setOnClickListener(this);
        
        SeekBar seekBar = (SeekBar) findViewById(R.id.volume_seek_bar);
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
    }
    
    private void setupNotification() {
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
            Log.e(LOG_TAG, e.toString());
       }
    }

    void doBindService() {
    	Intent serviceIntent = new Intent(this, PlaybackService.class);
    	serviceConnection = new ServiceConnection() {
	      @Override
	      public void onServiceConnected(ComponentName name, IBinder service) {
	    	  playbackService = ((PlaybackService.PlaybackBinder) service).getService();
	        Log.d(LOG_TAG, "CONNECTED");
	      }

	      @Override
	      public void onServiceDisconnected(ComponentName name) {
	        Log.w(LOG_TAG, "DISCONNECT");
	        playbackService  = null;
	      }
	    };
	    getApplicationContext().startService(serviceIntent);
	    getApplicationContext().bindService(serviceIntent, serviceConnection, 0);
       
        serviceIsBound = true;
    }

    void doUnbindService() {
        if (serviceIsBound) {
            // Detach our existing connection.
            unbindService(serviceConnection);
            serviceIsBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.play_button:
			playbackService.start();
			break;
		case R.id.stop_button:
			playbackService.stop();
			break;
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		int level = (int) Math.ceil(progress / 100.0 * max);
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, level, 0);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {}
	
	public void updateCurrentlyPlaying(ArrayList<Track> _recently_played, Track _now_playing) {
		now_playing = _now_playing;		
		nowPlayingText = (TextView) findViewById(R.id.now_playing);	
		playedAt = (TextView) findViewById(R.id.played_at);		

		nowPlayingText.post(new Runnable() {
	    	public void run() {
	    		now_playing_string += "<font color=#FCFC77>NOW PLAYING</font> &#183;" + " <b>ON-AIR:</b> " + now_playing.getDj() + "<br><br><hr>";
	    		now_playing_string += "<b>" + now_playing.getArtist() + "</b>" + " - " + now_playing.getTrack() + " <i>from " + 
	    			now_playing.getRelease() + " (" + now_playing.getLabel() + ")" + "</i>";				
	    		nowPlayingText.setText(Html.fromHtml(now_playing_string));	 
	    		playedAt.setText(now_playing.getPlayed_at_local().toString());
			}
		});
		
		recently_played = _recently_played;
		recentlyPlayedText = (TextView) findViewById(R.id.recently_played);		
		recentlyPlayedText.post(new Runnable() {
	    	public void run() {	    		
	    		recently_played_string += "<font color=#FCFC77>RECENTLY PLAYED</font>" + "<br>";

				for (int i = 0; i < recently_played.size(); ++i) {
					Track track = recently_played.get(i);
					recently_played_string += "<b>" + track.getArtist() + "</b>" + " - " + track.getTrack() + " <i>from " + 
		    			track.getRelease() + " (" + track.getLabel() + ")" + "</i>";				
					if (i < recently_played.size()-1) {
						recently_played_string += "<br><br><hr>";
					}						
		    	}
				recentlyPlayedText.setText(Html.fromHtml(recently_played_string));	  	    	
			}
		});
	}

	private BroadcastReceiver nowPlayingReceiver = new BroadcastReceiver () {
	    @Override
	    public void onReceive(Context arg0, Intent intent) {
	      Log.i("Playing.nowPlayingReceiver", "onReceive called");	
	      ArrayList<Track> recently_played = new ArrayList<Track>();;
	      Track now_playing = (Track) intent.getExtras().getSerializable("now_playing");
		  for (int i = 0; i < 3; ++i) {
			  recently_played.add((Track)intent.getExtras().getSerializable("recently_played"+String.valueOf(i)));
		  }
	      updateCurrentlyPlaying(recently_played, now_playing);
	    }
	};
	
    public void onResume() {
        super.onResume();
        registerReceiver(nowPlayingReceiver, new IntentFilter(ACTION_NOW_PLAYING_CHANGED));
    }

    public void onPause() {
        super.onPause();
        unregisterReceiver(nowPlayingReceiver);
    }    
}
