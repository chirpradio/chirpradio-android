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

import java.io.IOException;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.text.Html;
import android.util.Log;

/** Encapsulates a track (song). Includes constructors to create an object including from
* a JSONObject. Keeps a static variable with the current track being played and a method
* to fetch it.
*
* @author Chirag Patel
*/
public class Track {
	String dj; // "Stephen Dobek"
	String artist; // "Autre Ne Veut"
	String track; // "Drama Cum Drama",
	String label; // "Olde English Spelling Bee",
	Date played_at_gmt; // "2011-01-09T18:04:53.906564",
	String release; // "Autre Ne Veut",
	Date played_at_local; // "2011-01-09T12:04:53.906564-06:00",
	String id; // "agpjaGlycHJhZGlvchYLEg1QbGF5bGlzdEV2ZW50GMbqhQMM"

	static Track current_track; 
	/**
	 * @param dj
	 * @param artist
	 * @param track
	 * @param label
	 * @param played_at_gmt
	 * @param release
	 * @param played_at_local
	 * @param id
	 */
	public Track(String dj, String artist, String track, String label,
			Date played_at_gmt, String release, Date played_at_local,
			String id) {
		super();
		this.dj = dj;
		this.artist = artist;
		this.track = track;
		this.label = label;
		this.played_at_gmt = played_at_gmt;
		this.release = release;
		this.played_at_local = played_at_local;
		this.id = id;
	}

	public Track() {
		// TODO Auto-generated constructor stub
	}

	/** Constructs a track object from the JSON of a track
	* 
	* @throws JSONException, ParseException 
	* @author Chirag Patel
	*/
	public Track(JSONObject json_track) throws JSONException, ParseException {
		super();
		this.dj = json_track.getString("dj");
		this.artist = json_track.getString("artist");
		this.track = json_track.getString("track");
		this.label = json_track.getString("label");
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSSSS"); // "2011-01-09T18:04:53.906564"
		this.played_at_gmt = dateFormat.parse(json_track.getString("played_at_gmt")); 
		this.release = json_track.getString("release");
		dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSSSSZ"); // "2011-01-09T12:04:53.906564-06:00"
		//FIXME: played_at_local show up as "Sat Jan 29 21:08:43 America/Chicago 2011". Time is correctly shown as GMT but "America/Chicago" is not.
		this.played_at_local = dateFormat.parse(json_track.getString("played_at_local"));
		this.id = json_track.getString("id");
	}

	public String getDj() {
		return dj;
	}

	public void setDj(String dj) {
		this.dj = dj;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getTrack() {
		return track;
	}

	public void setTrack(String track) {
		this.track = track;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public Date getPlayed_at_gmt() {
		return played_at_gmt;
	}

	public void setPlayed_at_gmt(Date played_at_gmt) {
		this.played_at_gmt = played_at_gmt;
	}

	public String getRelease() {
		return release;
	}

	public void setRelease(String release) {
		this.release = release;
	}

	public Date getPlayed_at_local() {
		return played_at_local;
	}

	public void setPlayed_at_local(Date played_at_local) {
		this.played_at_local = played_at_local;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	private static final String LOG_TAG = NotificationUpdateTask.class.toString();

	/** Method to return the current playing track and store it in static member. Will attempt to fetch 
	* the track first, if it hasn't changed from the previous fetch (compares unique ID)
	* 
	* @return the current track. Returns null if it hasn't changed
	* @author Chirag Patel
	*/
	public static Track getCurrentTrack() {
		
		/*Documentation: http://code.google.com/p/chirpradio/wiki/TheChirpApi */
		HttpGet get = new HttpGet("http://chirpradio.appspot.com/api/current_playlist?src=chirpradio-android");
		DefaultHttpClient client = new DefaultHttpClient();
		
		
		try {
			HttpResponse response = client.execute(get);
			String responseBody = EntityUtils.toString(response.getEntity());

			// JSON response contains invalid 'comments', so strip them out
			responseBody = responseBody
					.replaceFirst("<!-- cached copy -->", "");
			responseBody = responseBody.replaceFirst(
					"<!-- end cached copy -->", "");
			responseBody = responseBody.replaceFirst("<!-- fresh copy -->", "");
			responseBody = responseBody.replaceFirst("<!-- end fresh copy -->",
					"");

			// Decode HTML entities
			responseBody = Html.fromHtml(responseBody).toString();

			try {
				JSONObject json_current_playlist = (JSONObject) new JSONTokener(responseBody).nextValue();
				JSONObject json_now_playing = json_current_playlist.getJSONObject("now_playing");
				String json_id = json_now_playing.getString("id");
				
				if (current_track == null || current_track.id == null || !current_track.id.equals(json_id)) {
				  current_track = new Track(json_now_playing);
				  return current_track;
				} else {
				  return null;
				}
			} catch (JSONException e) {
				Log.e(LOG_TAG, "", e);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				Log.e(LOG_TAG, "", e);
				e.printStackTrace();
			}
		} catch (ClientProtocolException e) {
			// a problem connecting or the connection was aborted
			Log.e(LOG_TAG, "", e);
		} catch (IOException e) {
			// an http protocol error
			Log.e(LOG_TAG, "", e);
		}
		
		//FIXME: Instead of returning null, return a track with Artist,Track,Release explaining the exception
		return null;
	}
}
