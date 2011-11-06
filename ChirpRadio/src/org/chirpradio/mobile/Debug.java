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
 * Wrapper around debug logging to simplify things and get fine
 * grained control
 *
 * @author Nick Gamroth
 */

package org.chirpradio.mobile;

import android.util.Log;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;

public class Debug
{
    private final static List<String> shutup;
    static {
        List<String> shh = new LinkedList<String>();
        shh.add("Request");
        shutup = Collections.unmodifiableList(shh);
    }

    private final static String LOG_TAG = "CHIRP";
    public static void log(Object caller, String message) {
        if(!shutup.contains(caller.getClass().getSimpleName()))
            Log.d(LOG_TAG, caller.getClass().getSimpleName() + ":\t" + message);
    }
}


