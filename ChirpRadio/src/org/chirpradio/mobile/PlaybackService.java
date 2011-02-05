// Copyright 2011 The Chicago Independent Radio Project 
// Copyright 2010 Google Inc.
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

import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class PlaybackService extends Service implements OnPreparedListener, OnErrorListener, OnCompletionListener {

	private static final String LOG_TAG = "PlaybackService"; 
	
	private MediaPlayer mediaPlayer;
	private Boolean isPrepared = false;
	private Boolean isPlaying = false;
	private Boolean isInCall = false;
	
	private PhoneStateListener phoneStateListener;
	
	private OnPlaybackStartedListener onPlaybackStartedListener;
	private OnPlaybackStoppedListener onPlaybackStoppedListener;	
	
	
	//	Service setup
	//
	
    public class PlaybackBinder extends Binder {
    	PlaybackService getService() {
            return PlaybackService.this;
        }
    }
    
	@Override
	public IBinder onBind(Intent intent) {
		return new PlaybackBinder();
	}

    @Override
    public void onCreate() {
    	Log.d(LOG_TAG, "created");
    	mediaPlayer = new MediaPlayer();
    	mediaPlayer.setOnPreparedListener(this);
    	mediaPlayer.setOnErrorListener(this);
    	mediaPlayer.setOnCompletionListener(this);
    	try {
			mediaPlayer.setDataSource("http://www.live365.com/play/chirpradio");
		} catch (IOException e) {
			e.printStackTrace();
		}
    	mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    	setupTelephonyHooks();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LOG_TAG, "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
    	Log.d(LOG_TAG, "DESTROYED");
    	mediaPlayer.stop();
    	mediaPlayer.release();
    	mediaPlayer = null;
    }
	
	public synchronized void start() {
		if (!isPrepared) {
			Log.d(LOG_TAG, "start, but not prepared");
			mediaPlayer.prepareAsync();
			return;
		} else {
			Log.d(LOG_TAG, "start");
			mediaPlayer.start();			
		}
	}
	
	public synchronized void stop() {
		mediaPlayer.stop();
		isPlaying = false;
		isPrepared = false;
		if (onPlaybackStoppedListener != null) {
			onPlaybackStoppedListener.onPlaybackStopped();
		}
	}


	//	MediaPlayer callbacks
	//
	
	@Override
	public void onPrepared(MediaPlayer mp) {
		Log.d(LOG_TAG, "prepared, starting media player");
		isPrepared = true;
		isPlaying = true;
		mediaPlayer.start();
		if (onPlaybackStartedListener != null) {
			onPlaybackStartedListener.onPlaybackStarted();
		}
	}
	
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Log.d(LOG_TAG, "ERROR! what: $(what) extra: $(extra)");
		return false; // Also call onCompletion
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		isPrepared = false;
		isPlaying = false;
	}
	

	public void setOnPlaybackStoppedListener(OnPlaybackStoppedListener listener) {
		onPlaybackStoppedListener = listener;
	}
	
	public void setOnPlaybackStartedListener(OnPlaybackStartedListener listener) {
		onPlaybackStartedListener = listener;
	}
	
	
	private void setupTelephonyHooks() {
		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

		phoneStateListener = new PhoneStateListener() {

		  @Override
		  public void onCallStateChanged(int state, String incomingNumber) {
		    switch (state) {
		    case TelephonyManager.CALL_STATE_OFFHOOK:
		    case TelephonyManager.CALL_STATE_RINGING:
		      if (isPlaying) {
		  		Log.d(LOG_TAG, "call began, stopping...");
		        stop();
		        isInCall = true;
		      }
		      break;
		    case TelephonyManager.CALL_STATE_IDLE:
		      if (isInCall) {
		    	 isInCall = false;
		    	Log.d(LOG_TAG, "call ended; resuming...");
		        start();
		      }
		      break;
		    }
		  }
		};

		telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
	}
	
}