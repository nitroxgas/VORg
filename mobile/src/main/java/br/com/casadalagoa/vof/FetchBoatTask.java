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

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

import br.com.casadalagoa.vof.data.BoatContract.BoatEntry;
import br.com.casadalagoa.vof.data.BoatContract.CodeEntry;

public class FetchBoatTask extends AsyncTask<String, Void, Void> {

    private final String LOG_TAG = FetchBoatTask.class.getSimpleName();
    private final Context mContext;

    public FetchBoatTask(Context context) {
        mContext = context;
    }

    /**
     * Helper method to handle insertion of a new code in the database.
     *
     * @param codeSetting The code string used to request updates from the server.
     * @param boatName Boat Mame
     * @param teamColor the latitude of the city
     * @return the row ID of the added code.
     */
    private long insertCodeInDatabase(
            String codeSetting, String boatName, String teamColor) {

        // First, check if the code with this city name exists in the db
        Cursor cursor = mContext.getContentResolver().query(
                CodeEntry.CONTENT_URI,
                new String[]{CodeEntry._ID},
                CodeEntry.COLUMN_CODE + " = ?",
                new String[]{codeSetting},
                null,
                null);

        if (cursor.moveToFirst()) {
            int codeIdIndex = cursor.getColumnIndex(CodeEntry._ID);
            return cursor.getLong(codeIdIndex);
        } else {
            ContentValues codeValues = new ContentValues();
            codeValues.put(CodeEntry.COLUMN_CODE, codeSetting);
            codeValues.put(CodeEntry.COLUMN_NAME, boatName);
            codeValues.put(CodeEntry.COLUMN_COLOR, teamColor);

            Uri codeInsertUri = mContext.getContentResolver()
                    .insert(CodeEntry.CONTENT_URI, codeValues);

            return ContentUris.parseId(codeInsertUri);
        }
    }

    /**
     * Take the String representing the complete Report in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private void getBoatDataFromJson(String ReportJsonStr)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.

        // Code information elements of "codes" array
        final String OWM_CODES = "codes";

        final String OWM_CODE  = "code";
        final String OWM_NAME  = "name";
        final String OWM_COLOR = "color";

        // Boat information.  Each Report info is an element of the "trackslatest" array.
        final String OWM_LATEST = "trackslatest";

        final String OWM_REPORTDATE = "reportdate";
        final String OWM_TIMEOFFIX = "timeoffix";
        final String OWM_STATUS = "status";
        final String OWM_LATITUDE = "latitude";
        final String OWM_LONGITUDE = "longitude";
        final String OWM_DTF = "dtf";
        final String OWM_DTLC = "dtlc";
        final String OWM_LEGSTANDING = "legstanding";
        final String OWM_TWENTYFOURHOURRUN = "twentyfourhourrun";
        final String OWM_LEGPROGRESS = "legprogress";
        final String OWM_DUL = "dul";
        final String OWM_BOATHEADINGTRUE = "boatheadingtrue";
        final String OWM_SMG = "smg";
        final String OWM_SEATEMPERATURE = "seatemperature";
        final String OWM_TRUWINDSPEEDAVG = "truwindspeedavg";
        final String OWM_SPEEDTHROWATER = "speedthrowater";
        final String OWM_TRUEWINDSPEEDMAX = "truewindspeedmax";
        final String OWM_TRUEWINDDIRECTION = "truewinddirection";
        final String OWM_LATESTSPEEDTHROWATER = "latestspeedthrowater";
        final String OWM_MAXAVGSPEED = "maxavgspeed";

        JSONObject ReportJson = new JSONObject(ReportJsonStr);

        JSONArray codeArray = ReportJson.getJSONArray(OWM_CODES);
        JSONObject dataArray = ReportJson.getJSONObject("data");
        String nextUpdate = ReportJson.getString("nextReport");
        JSONArray boatArray = dataArray.getJSONArray(OWM_LATEST);

        Vector<ContentValues> cVVector = new Vector<ContentValues>(codeArray.length());

        for(int i = 0; i < codeArray.length(); i++) {
            // These are the values that will be collected.
            String code, name, color;
            int codeId;

            JSONObject codeJson = codeArray.getJSONObject(i);
            code  = codeJson.getString(OWM_CODE);
            name  = codeJson.getString(OWM_NAME);
            color = codeJson.getString(OWM_COLOR);

            ContentValues codeValues = new ContentValues();

            codeValues.put(CodeEntry.COLUMN_CODE, code);
            codeValues.put(CodeEntry.COLUMN_NAME, name);
            codeValues.put(CodeEntry.COLUMN_COLOR, color);
            cVVector.add(codeValues);
            Log.v(LOG_TAG, "Code: "+ code + ", Name: " + name + " Color: " + color);
        }
        if (cVVector.size() > 0) {
            ContentValues[] cvArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cvArray);
            mContext.getContentResolver().bulkInsert(CodeEntry.CONTENT_URI, cvArray);
        }



        // Get and insert the new boat information into the database
        Vector<ContentValues> bVVector = new Vector<ContentValues>(21);

        for(int i = 0; i < boatArray.length(); i++) {
            // These are the values that will be collected.
            String code, reportdate, timeoffix, status, latitude, longitude, dtf, dtlc,
            legstanding, twentyfourhourrun, legprogress, dul, boatheadingtrue, smg,
            seatemperature, truwindspeedavg, speedthrowater, truewindspeedmax, truewinddirection, latestspeedthrowater,
            maxavgspeed;

            // Get the JSON object representing the day
            JSONArray dayForecast = boatArray.getJSONArray(i);

            // Just to be clear how to populate the vector
            code = dayForecast.getString(0);
            reportdate = dayForecast.getString(1);
            timeoffix = dayForecast.getString(2);
            status = dayForecast.getString(3);
            longitude = dayForecast.getString(4);
            latitude = dayForecast.getString(5);
            dtf = dayForecast.getString(6);
            dtlc = dayForecast.getString(7);
            legstanding = dayForecast.getString(8);
            twentyfourhourrun = dayForecast.getString(9);
            legprogress = dayForecast.getString(10);
            dul = dayForecast.getString(11);
            boatheadingtrue = dayForecast.getString(12);
            smg = dayForecast.getString(13);
            seatemperature = dayForecast.getString(14);
            truwindspeedavg = dayForecast.getString(15);
            speedthrowater = dayForecast.getString(16);
            truewindspeedmax = dayForecast.getString(17);
            truewinddirection = dayForecast.getString(18);
            latestspeedthrowater = dayForecast.getString(19);
            maxavgspeed = dayForecast.getString(19);

            ContentValues boatValues = new ContentValues();

            boatValues.put(BoatEntry.COLUMN_BOAT_ID, code);
            boatValues.put(BoatEntry.COLUMN_TIMEOFFIX, timeoffix);                  boatValues.put(BoatEntry.COLUMN_DUL, dul);
            boatValues.put(BoatEntry.COLUMN_REPORTDATE, reportdate);                boatValues.put(BoatEntry.COLUMN_BOATHEADINGTRUE, boatheadingtrue);
            boatValues.put(BoatEntry.COLUMN_STATUS, status);                        boatValues.put(BoatEntry.COLUMN_SMG, smg);
            boatValues.put(BoatEntry.COLUMN_LONGITUDE, longitude);                  boatValues.put(BoatEntry.COLUMN_SEATEMPERATURE, seatemperature);
            boatValues.put(BoatEntry.COLUMN_LATITUDE, latitude);                    boatValues.put(BoatEntry.COLUMN_TRUWINDSPEEDAVG, truwindspeedavg);
            boatValues.put(BoatEntry.COLUMN_DTF, dtf);                              boatValues.put(BoatEntry.COLUMN_SPEEDTHROWATER, speedthrowater);
            boatValues.put(BoatEntry.COLUMN_DTLC, dtlc);                            boatValues.put(BoatEntry.COLUMN_TRUEWINDSPEEDMAX, truewindspeedmax);
            boatValues.put(BoatEntry.COLUMN_LEG_STANDING, legstanding);             boatValues.put(BoatEntry.COLUMN_TRUEWINDDIRECTION, truewinddirection);
            boatValues.put(BoatEntry.COLUMN_TWENTYFOURHOURRUN, twentyfourhourrun);  boatValues.put(BoatEntry.COLUMN_LATESTSPEEDTHROWATER, latestspeedthrowater);
            boatValues.put(BoatEntry.COLUMN_LEGPROGRESS, legprogress);              boatValues.put(BoatEntry.COLUMN_MAXAVGSPEED, maxavgspeed);

            bVVector.add(boatValues);
            Log.v(LOG_TAG, "Data: "+ dayForecast.toString());
        }
        if (bVVector.size() > 0) {
            ContentValues[] cvArray = new ContentValues[bVVector.size()];
            bVVector.toArray(cvArray);
            mContext.getContentResolver().bulkInsert(BoatEntry.CONTENT_URI, cvArray);
        }
    }

    @Override
    protected Void doInBackground(String... params) {

      /*  // If there's no zip code, there's nothing to look up.  Verify size of params.
        if (params.length == 0) {
            return null;
        }
        String codeQuery = params[0];
*/
        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String ReportJsonStr = null;       

        try {
            // Construct the URL
            final String REPORT_BASE_URL =
                    "http://www.volvooceanrace.com/en/rdc/VOLVO_WEB_LEG1_2014.json";

            // Not really needed, but if the query someday needs parameters just add them here
            Uri builtUri = Uri.parse(REPORT_BASE_URL).buildUpon().build();

            URL url = new URL(builtUri.toString());

            // Create the request to OpenBoatMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            ReportJsonStr = buffer.toString();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the boat data, there's no point in attemping
            // to parse it.
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        try {
            getBoatDataFromJson(ReportJsonStr);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        // This will only happen if there was an error getting or parsing the Report.
        return null;
    }
}
