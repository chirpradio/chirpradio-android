package org.chirpradio.mobile;

import android.app.Activity;
import android.os.Bundle;

import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.chirpradio.mobile.NotificationUpdateService;

import org.chirpradio.mobile.NotificationUpdateTask;

public class MainMenu extends Activity implements OnClickListener {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);
        
        View playingButton = findViewById(R.id.playing_button);
        playingButton.setOnClickListener(this);
  
        
        NotificationUpdateService
        
//        ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(1);
//        NotificationUpdateTask task = new NotificationUpdateTask();
//        pool.scheduleAtFixedRate(task, 1000, 5000, TimeUnit.MILLISECONDS);
        
    }
    
    public void onClick(View v) {
    	switch (v.getId()) {
    	case R.id.playing_button:
    		Intent i = new Intent(this, Playing.class);
    		startActivity(i);
    		break;
    	}
    }
}