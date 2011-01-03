package org.chirpradio.mobile;

import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.media.MediaPlayer;

public class Playing extends Activity {

	MediaPlayer player;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.playing);
		playStream();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		player.stop();
	}
	
	public void playStream() {
		player = new MediaPlayer();
		try {
			player.setDataSource("http://www.live365.com/play/chirpradio");
			player.prepare();
		} catch (IOException e) {
		}
		player.start();
	}
	
}
