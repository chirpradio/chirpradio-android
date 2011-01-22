package org.chirpradio.mobile;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.Bundle;
import android.os.SystemClock;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import org.chirpradio.mobile.NotificationUpdateReceiver;

public class MainMenu extends Activity implements OnClickListener {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);
        
        View playingButton = findViewById(R.id.playing_button);
        playingButton.setOnClickListener(this);
        
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
            Log.e("MainMenu", e.toString());
       }
       
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