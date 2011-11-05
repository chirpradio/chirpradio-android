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
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.text.ParseException;

public class Playing extends Activity implements OnClickListener, OnSeekBarChangeListener, OnPlaybackStartedListener, OnPlaybackStoppedListener {

	public final static String ACTION_NOW_PLAYING_CHANGED = "org.chirpradio.mobile.TRACK_CHANGED";
	
    private PlaybackService playbackService;
    private ServiceConnection serviceConnection;
	private Boolean serviceIsBound;
	private AudioManager audioManager;
	ArrayList<Track> recentTracks;
	Track currentTrack;
	private TextView nowPlayingTextView;
    private TextView previous_0;
    private TextView previous_1;
    private TextView previous_2;
    private TextView previous_3;
    private TextView previous_4;
	//private TextView playedAtTextView;
    private View playButton;
    private View stopButton;
    private TextView playStatus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playing);
        
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setAudioLevel(80);
        
        doBindService();
        setupUICallbacks();
        setupNotification();
        findViewById(R.id.stop_button).setEnabled(false);
        
		nowPlayingTextView = (TextView) findViewById(R.id.now_playing);	
		//playedAtTextView = (TextView) findViewById(R.id.played_at);
        previous_0 = (TextView)findViewById(R.id.previous_0);
        previous_1 = (TextView)findViewById(R.id.previous_1);
        previous_2 = (TextView)findViewById(R.id.previous_2);
        previous_3 = (TextView)findViewById(R.id.previous_3);
        //previous_4 = (TextView)findViewById(R.id.previous_4);
        playStatus = (TextView)findViewById(R.id.play_status);

        // move this into a thread
        try {
            getPlaylist();
        } catch (Exception e) {
            Debug.log(this, "JSON Exception parsing playlist: " + e.toString());
        }
    }

    private void getPlaylist() throws JSONException, ParseException {
        String str = Request.sendRequest();
        Track t = new Track(new JSONObject(str).getJSONObject("now_playing"));
        nowPlayingTextView.setText(t.getArtist() + " - " + t.getTrack());

        JSONArray previous = new JSONObject(str).getJSONArray("recently_played");

        t = new Track(previous.getJSONObject(0));
        previous_0.setText(t.getArtist() + " - " + t.getTrack());
        t = new Track(previous.getJSONObject(1));
        previous_1.setText(t.getArtist() + " - " + t.getTrack());
        t = new Track(previous.getJSONObject(2));
        previous_2.setText(t.getArtist() + " - " + t.getTrack());
        t = new Track(previous.getJSONObject(3));
        previous_3.setText(t.getArtist() + " - " + t.getTrack());
        //t = new Track(previous.getJSONObject(4));
        //previous_4.setText(t.getArtist() + " - " + t.getTrack());
    }
    
    private void setupUICallbacks() {
        playButton = findViewById(R.id.play_button);
        playButton.setOnClickListener(this);
        stopButton = findViewById(R.id.stop_button);
        stopButton.setOnClickListener(this);
        
        //SeekBar seekBar = (SeekBar) findViewById(R.id.volume_seek_bar);
        //seekBar.setOnSeekBarChangeListener(this);
        int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int progress = (int) Math.ceil(volume * 100.0 / max);
        //seekBar.setProgress(progress);
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
            Debug.log(this, e.toString());
       }
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
	        playbackService = null;
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
            playStatus.setText("Bufferring Audio");
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

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		setAudioLevel(progress);
	}
	
	private void setAudioLevel(int level) {
		int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		int volume = (int) Math.ceil(level / 100.0 * max);
        Debug.log(this, "not setting audio volume like i should");
		//audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {}
	
	public void updateCurrentlyPlaying() {
		

		final String nowPlayingContent = "<font color=#FCFC77>NOW PLAYING</font> &#183;" + " <b>ON-AIR:</b> " +
										 currentTrack.getDj() + "<br><br><hr>" + "<b>" + currentTrack.getArtist() + "</b>" +
										 " - " + currentTrack.getTrack() + " <i>from " + currentTrack.getRelease() + " (" +
										 currentTrack.getLabel() + ")" + "</i>";
		
		nowPlayingTextView.post(new Runnable() {
	    	public void run() {
	    		nowPlayingTextView.setText(Html.fromHtml(nowPlayingContent));	 
//	    		playedAt.setText(now_playing.getPlayed_at_local().toString());
			}
		});
		
		String recentlyPlayedContent = "<font color=#FCFC77>RECENTLY PLAYED</font>" + "<br>";

		for (int i = 0; i < recentTracks.size(); ++i) {
			Track recentTrack = recentTracks.get(i);
			recentlyPlayedContent += "<b>" + recentTrack.getArtist() + "</b>" + " - " + recentTrack.getTrack() + " <i>from " + 
			recentTrack.getRelease() + " (" + recentTrack.getLabel() + ")" + "</i>";				
			if (i < recentTracks.size()-1) {
				recentlyPlayedContent += "<br><br><hr>";
			}						
    	}
		
		final String finalRecentlyPlayedContent = recentlyPlayedContent;
		
		//recentlyPlayedTextView.post(new Runnable() {
	   // 	public void run() {	    		
		//		recentlyPlayedTextView.setText(Html.fromHtml(finalRecentlyPlayedContent));	  	    	
		//	}
		//});
	}

	private BroadcastReceiver nowPlayingReceiver = new BroadcastReceiver () {
	    @Override
	    public void onReceive(Context arg0, Intent intent) {
	      Debug.log(this, "onReceive called");	
	      recentTracks = new ArrayList<Track>();;
	      currentTrack = (Track) intent.getExtras().getSerializable("now_playing");
		  for (int i = 0; i < 3; ++i) {
			  recentTracks.add((Track)intent.getExtras().getSerializable("recently_played"+String.valueOf(i)));
		  }
	      updateCurrentlyPlaying();
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

	@Override
	public void onPlaybackStopped() {
		runOnUiThread(new Runnable() {
		    public void run() {
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
		    	findViewById(R.id.stop_button).setEnabled(true);
                playStatus.setText("Playback started");
				Debug.log(this, "playback started");
		    }
		});
	}    
}
