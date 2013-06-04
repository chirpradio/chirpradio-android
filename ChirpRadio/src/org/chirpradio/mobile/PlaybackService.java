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
import android.content.Context;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.content.BroadcastReceiver;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import java.util.LinkedList;
import java.util.List;
import android.os.AsyncTask;
import java.io.BufferedReader;
import org.json.JSONObject;
import org.json.JSONArray;
import android.os.Handler;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Notification;

import org.npr.android.news.StreamProxy;

public class PlaybackService extends Service implements OnPreparedListener, OnErrorListener, OnCompletionListener {

	// This URL redirects to the real stream URL.
	private static final String STREAM_URL = "http://chirpradio.org/stream";

	private MediaPlayer mediaPlayer;
	private StreamProxy streamProxy;
	private Boolean isPrepared = false;
    private Boolean isPreparing = false;
	private Boolean isPlaying = false;
	private Boolean isInCall = false;
	private Boolean isStopping = false;
    private Boolean stopAfterPrepared = false;
    private Handler handler;
    private Boolean updatePlaylist;
    private Boolean headphonesInUse;

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
        Debug.log(this, "Binding to service");
		return new PlaybackBinder();
	}

    @Override
    public boolean onUnbind(Intent intent){
        Debug.log(this, "onUnbind");
        return false;
    }

    @Override
    public void onCreate() {
    	Debug.log(this, "created");
    	mediaPlayer = new MediaPlayer();
    	mediaPlayer.setOnPreparedListener(this);
    	mediaPlayer.setOnErrorListener(this);
    	mediaPlayer.setOnCompletionListener(this);

    	String playbackUrl;
        playbackUrl = STREAM_URL;

    	try {
			mediaPlayer.setDataSource(playbackUrl);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    	setupTelephonyHooks();

        // the headphone action gets called every time we get a start
        // message, i guess so i know the headphone state all the time.
        // so, i can assume there are no headphones plugged in, and the
        // broadcast receiver will set it to true if there are when
        // it gets notified the first time
        headphonesInUse = false;
    }

    public boolean isPlaying() {
        return isPlaying;
    }


    // Start on pre-2.0 systems
    @Override
    public void onStart(Intent intent, int startId) {
        Debug.log(this, "error - we shouldn't be starting on pre 2.0 devices");
        Debug.log(this, "Received start id " + startId + ": " + intent);
    }

    // Start on 2.0+ systems
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Debug.log(this, "Received start id " + startId + ": " + intent);
        registerReceiver(headphoneReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        Debug.log(this, "Starting get playlist task");
        updatePlaylist = true;
        handler = new Handler();
        handler.post(mUpdateTask);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
    	Debug.log(this, "DESTROYED");
    	mediaPlayer.stop();
    	mediaPlayer.release();
    	mediaPlayer = null;
    }

    private void notifyUi(String playlist) {
        Debug.log(this, "Notifying UI of playlist update");
        try {
            Track t = new Track(new JSONObject(playlist).getJSONObject("now_playing"));
            if(isPlaying)
                setNotification("CHIRP Radio", t.getArtist() + " - " + t.getTrack());
        } catch (Exception e) {
            Debug.log(this, playlist);
            Debug.log(this, "Error parsing now_playing: " + e.toString());
        }

        Intent intent = new Intent();
        intent.setAction("CHIRP");
        intent.putExtra("playlist", playlist);
        sendBroadcast(intent);
    }


	public synchronized void start() {
        Debug.log(this, "start called");
		if (isStopping) {
			Debug.log(this, "still stopping");
			return;
		}
		if (isPrepared) {
			Debug.log(this, "start");
			mediaPlayer.start();
		} else {
            // make sure we get to the newest part of the stream
            mediaPlayer.reset();
            try {
                mediaPlayer.setDataSource(STREAM_URL);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Debug.log(this, "start, but not prepared");
			if (streamProxy != null) {
    	        streamProxy.start();
			}
            Debug.log(this, "Preparing media player");
            isPreparing = true;
			mediaPlayer.prepareAsync();
			return;
		}
	}

	public synchronized void stop() {
        if(isPreparing) {
            stopAfterPrepared = true;
            return;
        }
        if (isStopping || !isPlaying) {
            Debug.log(this, "Stop playback called, but we're either stopped or not playing");
            return;
        }

		mediaPlayer.stop();
		isPlaying = false;
		isPrepared = false;
		isStopping = true;

        cancelNotification();

		// Stopping the StreamProxy can be slow, so do it in a different thread
		// so we don't block the UI
		new Thread(new Runnable() {
			public void run() {
				if (streamProxy != null) {
					streamProxy.stop();
				}
				isStopping = false;
				if (onPlaybackStoppedListener != null) {
					onPlaybackStoppedListener.onPlaybackStopped();
				}
			}
		}).start();
	}

	//	MediaPlayer callbacks
	//

	@Override
	public void onPrepared(MediaPlayer mp) {
        Debug.log(this, "prepared, starting media player");
        isPreparing = false;
		isPrepared = true;
		isPlaying = true;
        if(stopAfterPrepared) {
            // i should be able to leave the media player in the prepared state
            // to make subsequent start calls go faster
            Debug.log(this, "Prepare completed, but the user told us to stop already");
        }
        else {
            mediaPlayer.start();
            if (onPlaybackStartedListener != null) {
                onPlaybackStartedListener.onPlaybackStarted();
            }
        }
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Debug.log(this, "ERROR! what: " + what + " extra: " + extra);
		return false; // Also call onCompletion
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		if (onPlaybackStoppedListener != null) {
			onPlaybackStoppedListener.onPlaybackStopped();
		}
		Debug.log(this, "onCompletion");
		isPrepared = false;
		isPlaying = false;
	}

	public void setOnPlaybackStoppedListener(OnPlaybackStoppedListener listener) {
		onPlaybackStoppedListener = listener;
	}

	public void setOnPlaybackStartedListener(OnPlaybackStartedListener listener) {
		onPlaybackStartedListener = listener;
	}

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

	private void setupTelephonyHooks() {
		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

		phoneStateListener = new PhoneStateListener() {

		  @Override
		  public void onCallStateChanged(int state, String incomingNumber) {
		    switch (state) {
		    case TelephonyManager.CALL_STATE_OFFHOOK:
		    case TelephonyManager.CALL_STATE_RINGING:
		      if (isPlaying) {
		  		Debug.log(this, "call began, stopping...");
		        stop();
		        isInCall = true;
		      }
		      break;
		    case TelephonyManager.CALL_STATE_IDLE:
		      if (isInCall) {
		    	 isInCall = false;
		    	Debug.log(this, "call ended; resuming...");
		        start();
		      }
		      break;
		    }
		  }
		};

		telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
	}

    private BroadcastReceiver headphoneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            Debug.log(this, "headphone broadcast message received");
            if(intent.getAction().equalsIgnoreCase(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                if(state == 1)
                {
                    headphonesInUse = true;
                }
                if(state == 0 && headphonesInUse)
                {
                    Debug.log(this, "headphones unplugged - stopping playback");
                    Debug.log(this, "name: " + intent.getStringExtra("name"));
                    Debug.log(this, "microphone: " + intent.getIntExtra("microphone", -1));
                    headphonesInUse = false;
                    stop();
                }
            }
        }
    };

    // handles updating the get playlist task
    private Runnable mUpdateTask = new Runnable() {
        public void run() {
            if(updatePlaylist) {
                Debug.log(this, "updating playlist from timer");
                new GetPlaylistTask().execute();
            }
        }
    };

    private class GetPlaylistTask extends AsyncTask<Void, Integer, Boolean>
    {
        private boolean result = false;
        //private List<Track> playlist = new LinkedList<Track>();
        private String playlist;

        protected Boolean doInBackground(Void... no) {
            BufferedReader in = null;
            Boolean result = false;
            try {
                playlist = Request.sendRequest();
                result = true;
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
                //updateCurrentlyPlaying(playlist);
                notifyUi(playlist);
            }
            Debug.log(this, "Next playlist update in 20 seconds");
            //Track t = playlist.get(0);
            //setNotification("CHIRP Radio", t.getArtist() + " - " + t.getTrack());
            handler.postDelayed(mUpdateTask, 20000);
        }
    }
}
