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
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public class Playing extends Activity implements OnClickListener {

	private static final String LOG_TAG = "PlayingActivity";
	
    private PlaybackService mBoundService;
    private ServiceConnection mConnection;
	private Boolean mIsBound;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playing);
        doBindService();
        View playButton = findViewById(R.id.play_button);
        playButton.setOnClickListener(this);
        View stopButton = findViewById(R.id.stop_button);
        stopButton.setOnClickListener(this);        
    }

    void doBindService() {
    	Intent serviceIntent = new Intent(this, PlaybackService.class);
    	mConnection = new ServiceConnection() {
	      @Override
	      public void onServiceConnected(ComponentName name, IBinder service) {
	    	  mBoundService = ((PlaybackService.PlaybackBinder) service).getService();
	        Log.d(LOG_TAG, "CONNECTED");
	      }

	      @Override
	      public void onServiceDisconnected(ComponentName name) {
	        Log.w(LOG_TAG, "DISCONNECT");
	        mBoundService  = null;
	      }
	    };
	    getApplicationContext().startService(serviceIntent);
	    getApplicationContext().bindService(serviceIntent, mConnection, 0);
       
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
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
			mBoundService.start();
			break;
		case R.id.stop_button:
			mBoundService.stop();
			break;
		}
	}
	
}
