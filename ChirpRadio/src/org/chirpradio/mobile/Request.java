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

package com.j2ntech.matcher.util;

import java.util.Hashtable;
import java.net.URL;
import java.net.URLConnection;
import java.io.BufferedReader;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import org.json.JSONObject;
import com.j2ntech.matcher.MatcherActivity;

public class Request
{
    public static final String LOGIN = "login.php";
    public static final String LOCATION_SEARCH = "loc_search.php";
    public static final String SEND_MESSAGE = "send_message.php";
    public static final String GET_MESSAGES = "check_messages.php";
    public static final String REGISTER = "create_user.php";
    public static final String WHERE_AM_I = "whereami.php";
    public static final String CHECK_FOR_NOTES = "check_for_notes.php";
    public static final String LEAVE_NOTE = "leave_note.php";
    public static final String NAME_PLACE = "name_place.php";

    public static String sendRequest(Hashtable data, String page)
    {
        try {
            JSONObject loginJson = new JSONObject(data);
            String reqStr = "data=" + loginJson.toString();
            URL url = new URL(MatcherActivity.url + page);
            Log.getInstance().log(url.toString() + "?" + reqStr);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(reqStr);
            wr.flush();

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String str = in.readLine();
            // use this for debugging.  if str has a <br or something in it there's
            // probably a php warning
            String strdbg;
            while((strdbg = in.readLine()) != null)
                Log.getInstance().log(strdbg);
            wr.close();
            in.close();

            return str;
        } catch (Exception e) {
            Log.getInstance().log("exception during sendRequest");
            return "error";
        }
    }

}
