package br.com.casadalagoa.vorg.data;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Defines table and column names for the Boats database.
 */
public class BoatContract {

    // The "Content authority" is a name for the entire content provider, similar to the
    // relationship between a domain name and its website.  A convenient string to use for the
    // content authority is the package name for the app, which is guaranteed to be unique on the
    // device.
    public static final String CONTENT_AUTHORITY = "br.com.casadalagoa.vorg";

    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Possible paths (appended to base content URI for possible URI's)
    // For instance, content://com.example.android.sunshine.app/weather/ is a valid path for
    // looking at weather data. content://com.example.android.sunshine.app/givemeroot/ will fail,
    // as the ContentProvider hasn't been given any information on what to do with "givemeroot".
    // At least, let's hope not.  Don't be that dev, reader.  Don't be that dev.

    public static final String PATH_BOAT = "boat";
    public static final String PATH_CODE = "code";




    // Format used for storing dates in the database.  ALso used for converting those strings
    // back into date objects for comparison/processing.
    public static final String DATE_FORMAT = "yyyy-MM-dd HH-mm-ss";

    /**
     * Converts Date class to a string representation, used for easy comparison and database lookup.
     * @param date The input date
     * @return a DB-friendly representation of the date, using the format defined in DATE_FORMAT.
     */
    public static String getDbDateString(Date date){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        return sdf.format(date);
    }

    /**
     * Converts a dateText to a long Unix time representation
     * @param dateText the input date string
     * @return the Date object
     */
    public static Date getDateFromDb(String dateText) {
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(DATE_FORMAT);
        try {
            return dbDateFormat.parse(dateText);
        } catch ( ParseException e ) {
            e.printStackTrace();
            return null;
        }
    }

    /* Inner class that defines the table contents of the boats especification */
    public static final class CodeEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_CODE).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_CODE;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_CODE;

        // Table name
        public static final String TABLE_NAME = "code";

        // The code setting string is what will be sent to VOR Site
        // as the location query.
     //   public static final String COLUMN_CODE_SETTING = "code_setting";

        // Boat Code
        public static final String COLUMN_CODE = "code";

        // Human readable boat string
        public static final String COLUMN_NAME = "name";

        // Team Color
        public static final String COLUMN_COLOR = "color";

        public static Uri buildCodeUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    /* Inner class that defines the table contents of the boat table */
    public static final class BoatEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_BOAT).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_BOAT;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_BOAT;

        // Table name
        public static final String TABLE_NAME = "boat";

        // BOAT CODE NAME -> Boat ID
        public static final String COLUMN_BOAT_ID = "code";

        // Date, stored as Text with format yyyy-MM-dd hh:mm:ss
        public static final String COLUMN_REPORTDATE = "reportdate";

        // Date, stored as Text with format yyyy-MM-dd hh:mm:ss
        public static final String COLUMN_TIMEOFFIX = "timeoffixdate";

        // Status of the boat
        public static final String COLUMN_STATUS = "status";

        // Latitude x.xxxxx
        public static final String COLUMN_LATITUDE = "latitude";

        // Longitude x.xxxxx
        public static final String COLUMN_LONGITUDE = "longitude";

        // DTF Distance to finish
        public static final String COLUMN_DTF = "dtf";

        // Distance to L C ( First Competidor ?)
        public static final String COLUMN_DTLC = "dtlc";

        // LEG Position (10)
        public static final String COLUMN_LEG_STANDING = "legstanding";

        // 24h Run
        public static final String COLUMN_TWENTYFOURHOURRUN = "twentyfourhourrun";

        // Percent of leg progress
        public static final String COLUMN_LEGPROGRESS = "legprogress";

        // Distance ?
        public static final String COLUMN_DUL = "dul";

        // Real Boat Heading
        public static final String COLUMN_BOATHEADINGTRUE = "boatheadingtrue";

        // SMG
        public static final String COLUMN_SMG = "smg";

        // Sea Temperature
        public static final String COLUMN_SEATEMPERATURE = "seatemperature";

        // Avg of true wind speed
        public static final String COLUMN_TRUWINDSPEEDAVG = "truwindspeedavg";

        // Speed through water ? (18)
        public static final String COLUMN_SPEEDTHROWATER = "speedthrowater";

        // Max True Wind Speed
        public static final String COLUMN_TRUEWINDSPEEDMAX = "truewindspeedmax";

        // True Wind Direction Degrees are meteorological degrees (e.g, 0 is north, 180 is south).  Stored as floats.
        public static final String COLUMN_TRUEWINDDIRECTION = "truewinddirection";

        // Latest Speed Through Water
        public static final String COLUMN_LATESTSPEEDTHROWATER = "latestspeedthrowater";

        // Max Avg Speed
        public static final String COLUMN_MAXAVGSPEED = "maxavgspeed";


        public static Uri buildBoatUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildBoatCode(String locationSetting) {
            return CONTENT_URI.buildUpon().appendPath(locationSetting).build();
        }

       /* public static Uri buildBoatCodeWithStartDate(
                String locationSetting, String startDate) {
            return CONTENT_URI.buildUpon().appendPath(locationSetting)
                    .appendQueryParameter(COLUMN_DATETEXT, startDate).build();
        }*/

        public static Uri buildBoatCodeWithDate(String code, String date) {
            return CONTENT_URI.buildUpon().appendPath(code).appendPath(date).build();
        }

        public static String getCodeSettingFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static String getDateFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        /*public static String getStartDateFromUri(Uri uri) {
            return uri.getQueryParameter(COLUMN_DATETEXT);
        }*/
    }
}

