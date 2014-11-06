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
package br.com.casadalagoa.vof;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.preference.PreferenceManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import br.com.casadalagoa.vof.data.BoatContract;

public class Utility {

    public static String getPreferredBoat(Context context) {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        return mPrefs.getString(context.getString(R.string.pref_boat_key),context.getString(R.string.pref_boat_default));
    }

    public static void setPreferredBoat(Context context, String boat_pref) {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor mEditor = mPrefs.edit();
        mEditor.putString(context.getString(R.string.pref_boat_key), boat_pref).apply();
    }

    public static boolean hasDataToSync(Context context){
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            java.util.Date date;
            SimpleDateFormat date_f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            date_f.setTimeZone(TimeZone.getTimeZone("GMT00:00"));
            date = date_f.parse(mPrefs.getString(context.getString(R.string.pref_next_report), context.getString(R.string.pref_next_report_def)));
            //System.out.println(date);
            //System.out.println(mPrefs.getString(context.getString(R.string.pref_next_report), context.getString(R.string.pref_next_report_def)));
            long update_time = date.getTime();
            long now_time = System.currentTimeMillis();
            return now_time >= update_time;
        } catch (ParseException e) {
            System.out.println(">>>>>"+"date parsing exception");
        }
        return true;
    }

    public static String getNextUpdate(Context context) {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            java.util.Date date;
            SimpleDateFormat date_f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            date_f.setTimeZone(TimeZone.getTimeZone("GMT00:00"));
            date = date_f.parse(mPrefs.getString(context.getString(R.string.pref_next_report), context.getString(R.string.pref_next_report_def)));
            //System.out.println(date);
            //System.out.println(mPrefs.getString(context.getString(R.string.pref_next_report), context.getString(R.string.pref_next_report_def)));
            /*long update_time = date.getTime();
            long now_time = System.currentTimeMillis();*/
            //CharSequence text= DateUtils.getRelativeTimeSpanString(update_time, now_time, 0);
           // System.out.println("Differece: "+text.toString());
            date_f.applyPattern("HH:mm:ss");
            date_f.setTimeZone(TimeZone.getDefault());
            return date_f.format(date);
        } catch (ParseException e) {
            System.out.println(">>>>>"+"date parsing exception");
        }
        return "";
    }

    public static void setNextUpdate(Context context, String nextUpdate) {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor mEditor = mPrefs.edit();
        mEditor.putString(context.getString(R.string.pref_next_report), nextUpdate).apply();
    }

    public static String[] getBoatArray(Context context, String boat_pref){

        String[] return_str = {"_id","b_code","reportdate","timeoffixdate","status","latitude","longitude","dtf","dtlc","legstanding","twentyfourhourrun","legprogress","dul","boatheadingtrue","smg","seatemperature","truwindspeedavg","speedthrowater","truewindspeedmax","truewinddirection","latestspeedthrowater","maxavgspeed","_id","code","name","color"};
        Cursor boat_data = context.getContentResolver().query(BoatContract.BoatEntry.buildBoatCode(boat_pref),null,null,null,null);
        for (boat_data.moveToFirst(); !boat_data.isAfterLast(); boat_data.moveToNext()) {
            for (int i = 0; i < 26; i++) {
                return_str[i] = boat_data.getString(i);
                if (return_str[i].contains("null")) return_str[i]="NA";
                String tmp_str;
                if ((i==5)||(i==6)){
                    tmp_str = Location.convert(Double.valueOf(boat_data.getString(i)), Location.FORMAT_SECONDS);
                    if (i==6) {
                        if (tmp_str.startsWith("-"))
                            tmp_str = tmp_str.replace("-", " ") + " S";
                         else
                            tmp_str = tmp_str.replace("+", " ") + " N";
                        } else
                         if (tmp_str.startsWith("-"))
                            tmp_str=tmp_str.replace("-"," ") + " W";
                        else
                            tmp_str=tmp_str.replace("+"," ") + " E";
                    tmp_str=tmp_str.replaceFirst(":","º");
                    tmp_str=tmp_str.replaceFirst(":","'");
                    int idx = tmp_str.lastIndexOf(",");
                    if (idx>0) tmp_str=tmp_str.substring(0,idx);
                    tmp_str+="\"";
                    return_str[i]=tmp_str;
                }
            }
        }
        boat_data.close();
        //  0    1     2          3             4      5        6         7    8   9             10               11         12   13             14   15             16             17               18              19                  20                  21        22  23    24  25
        //[_id,b_code,reportdate,timeoffixdate,status,latitude,longitude,dtf,dtlc,legstanding,twentyfourhourrun,legprogress,dul,boatheadingtrue,smg,seatemperature,truwindspeedavg,speedthrowater,truewindspeedmax,truewinddirection,latestspeedthrowater,maxavgspeed,_id,code,name,color]
        return return_str;
    }

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
        String boat_heading;
        if (!Heading.contains("null")) {
            boat_heading = boat_code + "_"+getWindHeading(Float.valueOf(Heading)) + "0001";
        } else
            boat_heading = boat_code + "_e0001";
        String packageName = context.getPackageName();
        return context.getResources().getIdentifier(boat_heading.toLowerCase(), "drawable", packageName);
    }
}


/*

    public static String getFormattedWind(Context context, float windSpeed, float degrees) {
        int windFormat;
        if (Utility.isMetric(context)) {
            windFormat = R.string.format_wind_kmh;
        } else {
            windFormat = R.string.format_wind_mph;
            windSpeed = .621371192237334f * windSpeed;
        }
        String direction = getWindHeading(degrees);
        return String.format(context.getString(windFormat), windSpeed, direction);
    }
*/

/**
 * Helper method to provide the icon resource id according to the weather condition id returned
 * by the OpenWeatherMap call.
 * //@param weatherId from OpenWeatherMap API response
 * @return resource id for the corresponding icon. -1 if no relation is found.
 */
    /*
    public static int getIconResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        *//*
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_cloudy;
        }
        *//*
        return R.drawable.alvi_e0001;
    }


    public static int getArtResourceForBoat(int boatId) {

        return R.drawable.alvi_e0001;

    }*/


/*

    public static boolean isMetric(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_units_key),
                context.getString(R.string.pref_units_metric))
                .equals(context.getString(R.string.pref_units_metric));
    }
*/

 /*   public static String formatTemperature(Context context, double temperature, boolean isMetric) {
        double temp;
        if ( !isMetric ) {
            temp = 9*temperature/5+32;
        } else {
            temp = temperature;
        }
        return context.getString(R.string.format_temperature, temp);
    }

    static String formatDate(String dateString) {
        Date date = BoatContract.getDateFromDb(dateString);
        return DateFormat.getDateInstance().format(date);
    }
*/
// Format used for storing dates in the database.  ALso used for converting those strings
// back into date objects for comparison/processing.
// public static final String DATE_FORMAT = "yyyyMMdd";
/*

    */
/**
 * Helper method to convert the database representation of the date into something to display
 * to users.  As classy and polished a user experience as "20140102" is, we can do better.
 *
 * @param context Context to use for resource localization
 * @param dateStr The db formatted date string, expected to be of the form specified
 *                in Utility.DATE_FORMAT
 * @return a user-friendly representation of the date.
 *//*

    public static String getFriendlyDayString(Context context, String dateStr) {
        // The day string for forecast uses the following logic:
        // For today: "Today, June 8"
        // For tomorrow:  "Tomorrow"
        // For the next 5 days: "Wednesday" (just the day name)
        // For all days after that: "Mon Jun 8"

        Date todayDate = new Date();
        String todayStr = BoatContract.getDbDateString(todayDate);
        Date inputDate = BoatContract.getDateFromDb(dateStr);

        // If the date we're building the String for is today's date, the format
        // is "Today, June 24"
        if (todayStr.equals(dateStr)) {
            String today = context.getString(R.string.today);
            int formatId = R.string.format_full_friendly_date;
            return String.format(context.getString(
                    formatId,
                    today,
                    getFormattedMonthDay(context, dateStr)));
        } else {
            Calendar cal = Calendar.getInstance();
            cal.setTime(todayDate);
            cal.add(Calendar.DATE, 7);
            String weekFutureString = BoatContract.getDbDateString(cal.getTime());

            if (dateStr.compareTo(weekFutureString) < 0) {
                // If the input date is less than a week in the future, just return the day name.
                return getDayName(context, dateStr);
            } else {
                // Otherwise, use the form "Mon Jun 3"
                SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
                return shortenedDateFormat.format(inputDate);
            }
        }
    }
*/
/*

    */
/**
 * Given a day, returns just the name to use for that day.
 * E.g "today", "tomorrow", "wednesday".
 *
 * //@param context Context to use for resource localization
 * //@param dateStr The db formatted date string, expected to be of the form specified
 *                in Utility.DATE_FORMAT
 * @return
 *//*

    public static String getDayName(Context context, String dateStr) {
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(Utility.DATE_FORMAT);
        try {
            Date inputDate = dbDateFormat.parse(dateStr);
            Date todayDate = new Date();
            // If the date is today, return the localized version of "Today" instead of the actual
            // day name.
            if (BoatContract.getDbDateString(todayDate).equals(dateStr)) {
                return context.getString(R.string.today);
            } else {
                // If the date is set for tomorrow, the format is "Tomorrow".
                Calendar cal = Calendar.getInstance();
                cal.setTime(todayDate);
                cal.add(Calendar.DATE, 1);
                Date tomorrowDate = cal.getTime();
                if (BoatContract.getDbDateString(tomorrowDate).equals(
                        dateStr)) {
                    return context.getString(R.string.tomorrow);
                } else {
                    // Otherwise, the format is just the day of the week (e.g "Wednesday".
                    SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
                    return dayFormat.format(inputDate);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
            // It couldn't process the date correctly.
            return "";
        }
    }

    */
/**
 * Converts db date format to the format "Month day", e.g "June 24".
 * //@param context Context to use for resource localization
 * //@param dateStr The db formatted date string, expected to be of the form specified
 *                in Utility.DATE_FORMAT
 * @return The day in the form of a string formatted "December 6"
 *//*

    public static String getFormattedMonthDay(Context context, String dateStr) {
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(Utility.DATE_FORMAT);
        try {
            Date inputDate = dbDateFormat.parse(dateStr);
            SimpleDateFormat monthDayFormat = new SimpleDateFormat("MMMM dd");
            String monthDayString = monthDayFormat.format(inputDate);
            return monthDayString;
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
*/
