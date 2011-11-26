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

/**
 * Class to encapsulate network requests.  Only handles GET requests since
 * that's all chirp uses right now.  
 *
 * @author Nick Gamroth
 */

package org.chirpradio.mobile;

import java.util.Hashtable;
import java.net.URL;
import java.net.URLConnection;
import java.io.BufferedReader;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import org.json.JSONObject;

public class Request
{
    private final static String baseUrl = "http://chirpradio.appspot.com/api/";

    public final static String CURRENT_PLAYLIST = "current_playlist";

    public static String sendRequest()
    {
        try {
            String reqStr = baseUrl + CURRENT_PLAYLIST + "?src=chirpradio-android";
            URL url = new URL(reqStr);
            //Debug.log("Request", "Requesting " + reqStr);
            URLConnection conn = url.openConnection();

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String str = in.readLine();
            in.close();

            return str;
        } catch (Exception e) {
            Debug.log("Request", "exception during sendRequest");
            return "error";
        }
    }

}
