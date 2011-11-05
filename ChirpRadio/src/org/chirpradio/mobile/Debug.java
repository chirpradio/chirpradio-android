package org.chirpradio.mobile;

import android.util.Log;

public class Debug
{
    private final static String LOG_TAG = "CHIRP";
    public static void log(Object caller, String message) {
        Log.d(LOG_TAG, caller.getClass().getSimpleName() + ":\t" + message);
    }
}


