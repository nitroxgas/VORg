/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.com.casadalagoa.vorf;

import android.content.Context;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utility {

    public static String getWindHeading(float degrees){
        // From wind direction in degrees, determine compass direction as a string (e.g NW)
        // You know what's fun, writing really long if/else statements with tons of possible
        // conditions.  Seriously, try it!
        String direction = "Unknown";
        if (degrees >= 337.5 || degrees < 22.5) {
            direction = "N";
        } else if (degrees >= 22.5 && degrees < 67.5) {
            direction = "NE";
        } else if (degrees >= 67.5 && degrees < 112.5) {
            direction = "E";
        } else if (degrees >= 112.5 && degrees < 157.5) {
            direction = "SE";
        } else if (degrees >= 157.5 && degrees < 202.5) {
            direction = "S";
        } else if (degrees >= 202.5 && degrees < 247.5) {
            direction = "SW";
        } else if (degrees >= 247.5 && degrees < 292.5) {
            direction = "W";
        } else if (degrees >= 292.5 || degrees < 22.5) {
            direction = "NW";
        }
        return direction;
    }

    public static int getFormattedBoatHeading(Context context, String boat_code, String Heading) {
        String boat_heading="";
        if ((!Heading.contains("NA"))&&(!Heading.contains("boatheadingtrue"))) {
            boat_heading = boat_code + "_"+getWindHeading(Float.valueOf(Heading)) + "0001";
        } else
            boat_heading = boat_code + "_e0001";
        String packageName = context.getPackageName();
        return context.getResources().getIdentifier(boat_heading.toLowerCase(), "drawable", packageName);
    }

    public static Date NextEventDate(String nextEventStr){
        try {
            java.util.Date date;
            SimpleDateFormat date_f = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss ZZZ");
            date = date_f.parse(nextEventStr);
            return date;
        } catch (ParseException e) {
            System.out.println(">>>>>"+"date parsing exception");
        }
        return null;
    }

}
