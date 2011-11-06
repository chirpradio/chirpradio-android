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

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.text.format.DateUtils;
import android.util.Log;

/** Encapsulates a track (song). Includes constructors to create an object including from
* a JSONObject. Keeps a static variable with the current track being played and a method
* to fetch it.
*
* @author Chirag Patel
*/
public class Track implements Serializable {

	private static final long serialVersionUID = 1L;
	String dj; // "Stephen Dobek"
	String artist; // "Autre Ne Veut"
	String track; // "Drama Cum Drama",
	String label; // "Olde English Spelling Bee",
	Date played_at_gmt; // "2011-01-09T18:04:53.906564",
	String release; // "Autre Ne Veut",
	Date played_at_local; // "2011-01-09T12:04:53.906564-06:00",
	String id; // "agpjaGlycHJhZGlvchYLEg1QbGF5bGlzdEV2ZW50GMbqhQMM"

	static Hashtable<String, Serializable> hashtable = new Hashtable<String, Serializable>();
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

	/**
	 * @param dj
	 * @param artist
	 * @param id
	 */
	public Track(String dj, String artist, String id) {
		super();
		this.dj = dj;
		this.artist = artist;
		this.track = "";
		this.label = "";
		this.played_at_gmt = null;
		this.release = "";
		this.played_at_local = null;
		this.id = id;
	}

	public Track() {
		// TODO Auto-generated constructor stub
	}

    public Track(String str) {
        super();
        try {
            parseTrack(new JSONObject(str));
        } catch(Exception e) {
            Debug.log(this, "Exception parsing track: " + e.toString());
        }
    }

	/** Constructs a track object from the JSON of a track
	* 
	* @throws JSONException, ParseException 
	* @author Chirag Patel
	*/
	public Track(JSONObject json_track) throws JSONException, ParseException {
		super();
        try {
            parseTrack(json_track);
        } catch(Exception e) {
            Debug.log(this, "Exception parsing track: " + e.toString());
        }
	}

    private void parseTrack(JSONObject json_track) throws JSONException, ParseException {
        //Debug.log(this, json.get(name).toString());
        //JSONObject json_track = new JSONObject(json.get("now_playing").toString());
		this.dj = json_track.getString("dj");
		this.artist = json_track.getString("artist");
		this.track = json_track.getString("track");
		this.label = json_track.getString("label");
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSSSS"); // "2011-01-09T18:04:53.906564"
		this.played_at_gmt = dateFormat.parse(json_track.getString("played_at_gmt")); 
		this.release = json_track.getString("release");
		
		//DateTimeFormatter parser2 = ISODateTimeFormat.dateTimeNoMillis();
		//String jtdate = "2010-01-01T12:00:00+01:00";
		//System.out.println(parser2.parseDateTime(jtdate));
		
		//String pattern = DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern();
		//Date d = DateUtils.parseDate(json_track.getString("played_at_gmt"), new String[] { pattern });
		
		dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ"); // "2011-01-09T12:04:53.906564-06:00"
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

}
