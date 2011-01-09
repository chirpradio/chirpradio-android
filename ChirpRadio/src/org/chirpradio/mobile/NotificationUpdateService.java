package org.chirpradio.mobile;

import android.app.Service;

import android.content.Intent;
import android.os.IBinder;

import android.util.Log;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;

public class NotificationUpdateService extends Service {
	
	private NotificationManager mNM;
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification();
    }

//    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(1);

        Log.i("LocalService", "Stopped");
    }
	
    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = "Service Started";

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.icon, text, System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, "Service Started", text, contentIntent);

        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        mNM.notify(1, notification);
    }

}
