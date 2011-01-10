package org.chirpradio.mobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.chirpradio.mobile.NotificationUpdateTask;

public class NotificationUpdateReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("NotificationUpdateReceiver", "onReceive called");
		new NotificationUpdateTask().execute(context);
	}

}
