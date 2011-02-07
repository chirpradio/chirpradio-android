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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class Playing extends Activity implements OnClickListener, OnSeekBarChangeListener {

	private static final String LOG_TAG = "PlayingActivity";
	
    private PlaybackService playbackService;
    private ServiceConnection serviceConnection;
	private Boolean serviceIsBound;
	private AudioManager audioManager;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playing);
        
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        
        doBindService();
        setupUICallbacks();
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
	
}
