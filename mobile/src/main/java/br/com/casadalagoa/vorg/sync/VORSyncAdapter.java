package br.com.casadalagoa.vorg.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Vector;

import br.com.casadalagoa.vorg.MainActivity;
import br.com.casadalagoa.vorg.R;
import br.com.casadalagoa.vorg.Utility;
import br.com.casadalagoa.vorg.data.BoatContract.BoatEntry;
import br.com.casadalagoa.vorg.data.BoatContract.CodeEntry;

public class VORSyncAdapter extends AbstractThreadedSyncAdapter  implements // DataApi.DataListener,
        MessageApi.MessageListener, NodeApi.NodeListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;

    public final String LOG_TAG = VORSyncAdapter.class.getSimpleName();

    // Interval at which to sync with the weather, in milliseconds.
    // 1000 milliseconds (1 second) * 60 seconds (1 minute) * 180 = 3 hours
    public static final int SYNC_INTERVAL = 1000 * 60 * 5;// * 60;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;

    private final Context mContext;

    public VORSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        Log.v(LOG_TAG, "Creating SyncAdapter");
        mContext = context;
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            // Optionally, add additional APIs and scopes if required.
            //
        }
        mGoogleApiClient.connect();
        Log.v(LOG_TAG, "Connecting GoogleApiClient");
    }


    @Override
    public void onPerformSync(Account account, Bundle bundle, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        Log.d(LOG_TAG, "Starting sync");
        // Getting the zipcode to send to the API
        String locationQuery = Utility.getPreferredBoat(mContext);

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
                return;
            }
            ReportJsonStr = buffer.toString();
            parseJSON(ReportJsonStr);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the boat data, there's no point in attemping
            // to parse it.
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
                Log.d(LOG_TAG, "Exit closing stream");
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
    }

    public void parseJSON(String ReportJsonStr){

        // Now we have a String representing the complete forecast in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.

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
        if (ReportJsonStr!=null) {
            try {

                JSONObject ReportJson = new JSONObject(ReportJsonStr);

                JSONArray codeArray = ReportJson.getJSONArray(OWM_CODES);
                JSONObject dataArray = ReportJson.getJSONObject("data");
                String nextUpdate = ReportJson.getString("nextReport");
                JSONArray boatArray = dataArray.getJSONArray(OWM_LATEST);

                Vector<ContentValues> cVVector = new Vector<ContentValues>(codeArray.length());

                for (int i = 0; i < codeArray.length(); i++) {
                    // These are the values that will be collected.
                    String code, name, color;
                    int codeId;

                    JSONObject codeJson = codeArray.getJSONObject(i);
                    code = codeJson.getString(OWM_CODE);
                    name = codeJson.getString(OWM_NAME);
                    color = codeJson.getString(OWM_COLOR);

                    ContentValues codeValues = new ContentValues();

                    codeValues.put(CodeEntry.COLUMN_CODE, code);
                    codeValues.put(CodeEntry.COLUMN_NAME, name);
                    codeValues.put(CodeEntry.COLUMN_COLOR, color);
                    cVVector.add(codeValues);
                    Log.v(LOG_TAG, "Code: " + code + ", Name: " + name + " Color: " + color);
                }
                if (cVVector.size() > 0) {
                    ContentValues[] cvArray = new ContentValues[cVVector.size()];
                    cVVector.toArray(cvArray);
                    mContext.getContentResolver().bulkInsert(CodeEntry.CONTENT_URI, cvArray);
                }


                // Get and insert the new boat information into the database
                Vector<ContentValues> bVVector = new Vector<ContentValues>(21);

                for (int i = 0; i < boatArray.length(); i++) {
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
                    boatValues.put(BoatEntry.COLUMN_TIMEOFFIX, timeoffix);
                    boatValues.put(BoatEntry.COLUMN_DUL, dul);
                    boatValues.put(BoatEntry.COLUMN_REPORTDATE, reportdate);
                    boatValues.put(BoatEntry.COLUMN_BOATHEADINGTRUE, boatheadingtrue);
                    boatValues.put(BoatEntry.COLUMN_STATUS, status);
                    boatValues.put(BoatEntry.COLUMN_SMG, smg);
                    boatValues.put(BoatEntry.COLUMN_LONGITUDE, longitude);
                    boatValues.put(BoatEntry.COLUMN_SEATEMPERATURE, seatemperature);
                    boatValues.put(BoatEntry.COLUMN_LATITUDE, latitude);
                    boatValues.put(BoatEntry.COLUMN_TRUWINDSPEEDAVG, truwindspeedavg);
                    boatValues.put(BoatEntry.COLUMN_DTF, dtf);
                    boatValues.put(BoatEntry.COLUMN_SPEEDTHROWATER, speedthrowater);
                    boatValues.put(BoatEntry.COLUMN_DTLC, dtlc);
                    boatValues.put(BoatEntry.COLUMN_TRUEWINDSPEEDMAX, truewindspeedmax);
                    boatValues.put(BoatEntry.COLUMN_LEG_STANDING, legstanding);
                    boatValues.put(BoatEntry.COLUMN_TRUEWINDDIRECTION, truewinddirection);
                    boatValues.put(BoatEntry.COLUMN_TWENTYFOURHOURRUN, twentyfourhourrun);
                    boatValues.put(BoatEntry.COLUMN_LATESTSPEEDTHROWATER, latestspeedthrowater);
                    boatValues.put(BoatEntry.COLUMN_LEGPROGRESS, legprogress);
                    boatValues.put(BoatEntry.COLUMN_MAXAVGSPEED, maxavgspeed);

                    bVVector.add(boatValues);
                    Log.v(LOG_TAG, "Data: " + dayForecast.toString());
                }
                if (bVVector.size() > 0) {
                    ContentValues[] cvArray = new ContentValues[bVVector.size()];
                    bVVector.toArray(cvArray);
                    mContext.getContentResolver().bulkInsert(BoatEntry.CONTENT_URI, cvArray);
                }
                Log.d(LOG_TAG, "Sync Data Complete. " + cVVector.size() + " Inserted");
                sendData(Utility.getBoatArray(mContext,"VEST"));
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
        } else Log.d(LOG_TAG, "No String to parse!");
        //return;
    }


    private void notifyBoatData(double high, double low, String description, int weatherId) {
        Context context = getContext();
        //checking the last update and notify if it' the first of the day
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);

        // If notifications are enabled in preferences...
        boolean defaultForNotifications =
                Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default));
        boolean notificationsEnabled =
                prefs.getBoolean(displayNotificationsKey, defaultForNotifications);

        // AND it's been at least 24h since the last notification was displayed
        String lastNotificationKey = context.getString(R.string.pref_last_notification);
        long lastNotification = prefs.getLong(lastNotificationKey, 0);
        boolean timeToNotify = (System.currentTimeMillis() - lastNotification >= DAY_IN_MILLIS);

        if (notificationsEnabled && timeToNotify) {
            // Last sync was more than 1 day ago, let's send a notification with the weather.

            int iconId = Utility.getIconResourceForWeatherCondition(weatherId);
            String title = mContext.getString(R.string.app_name);

            boolean isMetric = Utility.isMetric(mContext);

            // Define the text of the forecast.
            String contentText = String.format(mContext.getString(R.string.format_notification),
                    description,
                    Utility.formatTemperature(mContext, high, isMetric),
                    Utility.formatTemperature(mContext, low, isMetric));

            // NotificationCompatBuilder is a very convenient way to build backward-compatible
            // notifications.  Just throw in some data.
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(mContext)
                            .setSmallIcon(iconId)
                            .setContentTitle(title)
                            .setContentText(contentText);

            // Make something interesting happen when the user clicks on the notification.
            // In this case, opening the app is sufficient.
            Intent resultIntent = new Intent(mContext, MainActivity.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
            stackBuilder.addParentStack(MainActivity.class);
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) getContext()
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            // mId allows you to update the notification later on.
          //  mNotificationManager.notify(WEATHER_NOTIFICATION_ID, mBuilder.build());

            //refreshing last sync
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong(lastNotificationKey, System.currentTimeMillis());
            editor.commit();
        }
    }

     /**
     * Helper method to have the sync adapter sync immediately
     *
     * @param context An app context
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
        Log.v("VORGSyncAdapter:", "Sync Performed");

    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {

        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
        Log.v("VORGSyncAdapter:", "Periodic Sync Configured");
    }

    /**
    * Helper method to get the fake account to be used with SyncAdapter, or make a new one
    * if the fake account doesn't exist yet.
    *
    * @param context The context used to access the account service
    * @return a fake account.
    */
    public static Account getSyncAccount(Context context) {
        Log.v("VORGSyncAdapter:", "Getting Sync Account");
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

            // If the password doesn't exist, the account doesn't exist
            if (null == accountManager.getPassword(newAccount)) {
                // Add the account and account type, no password or user data
                // If successful, return the Account object, otherwise report an error.
                if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                    return null;
                }

                // If you don't set android:syncable="true" in
                // in your <provider> element in the manifest,
                // then call context.setIsSyncable(account, AUTHORITY, 1)
                // here.
                onAccountCreated(newAccount, context);
            }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context) {

        // Schedule the sync for periodic execution
        VORSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        // Without calling setSyncAutomatically, our periodic sync will not be enabled.
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        // Let's do a sync to get things started.
        syncImmediately(context);
        Log.v("VORGSyncAdapter:", "Account Created");
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
        Log.v("VORSyncAdapter:", "Initializing SyncAdapter");
    }

    //  ###### Wear Connection Implementation
    private static final String TAG = "VOR_watchface";

    private static final String KEY_IN_RESOLUTION = "is_in_resolution";
    private static final int REQUEST_RESOLVE_ERROR = 1000;

    // Patch and data keys to send to watch
    private static final String BD_PATH = "/bd"; // Boat Data
    private static final String BD_KEY = "bd"; // Boat data array!

    private static int count = 5;


    /**
     * Request code for auto Google Play Services error resolution.
     */
    protected static final int REQUEST_CODE_RESOLUTION = 1;

    /**
     * Google API client.
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * Determines if the client is in a resolution state, and
     * waiting for resolution intent to return.
     */
    private boolean mIsInResolution;

/*

    *//**
     * Called when activity gets invisible. Connection to Play Services needs to
     * be disconnected as soon as an activity is invisible.
     *//*
    @Override
    protected void onStop() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    *//**
     * Saves the resolution state.
     *//*
    @Override

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IN_RESOLUTION, mIsInResolution);
    }*/

  /*  *//**
     * Handles Google Play Services resolution callbacks.
     *//*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_RESOLUTION:
                retryConnecting();
                break;
        }
    }*/

    private void retryConnecting() {
        Log.v(TAG, "RetryConnecting");
        mIsInResolution = false;
        if (!mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }

    /**
     * Called when {@code mGoogleApiClient} is connected.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.v(TAG, "GoogleApiClient connected");
        // TODO: Start making API requests.
       // Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.NodeApi.addListener(mGoogleApiClient, this);
    }

    /**
     * Called when {@code mGoogleApiClient} connection is suspended.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.v(TAG, "GoogleApiClient connection suspended");
        retryConnecting();
    }

    /**
     * Called when {@code mGoogleApiClient} is trying to connect but failed.
     * Handle {@code result.getResolution()} if there is a resolution
     * available.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.v(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // Show a localized error dialog.
            return;
        }
        // If there is an existing resolution error being displayed or a resolution
        // activity has started before, do nothing and wait for resolution
        // progress to be completed.
        if (mIsInResolution) {
            return;
        }
        mIsInResolution = true;
        retryConnecting();

    }

  /*  @Override //DataListener
    public void onDataChanged(DataEventBuffer dataEvents) {
        *//*final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();*//*
        Log.v(TAG, "Data Changed");
    }*/

    @Override //MessageListener
    public void onMessageReceived(final MessageEvent messageEvent) {
        Log.v(TAG, "Message Received!");
        // A message from watch was received
        /*
        Implement the handler if UI interaction will be necessary
        mHandler.post(new Runnable() {
            @Override
            public void run() {

            }
        });*/

    }

    @Override //NodeListener
    public void onPeerConnected(final Node peer) {
        Log.v(TAG, "Peer Connected! ");
        // onPeerConnected:
        /*mHandler.post(new Runnable() {
            @Override
            public void run() {

            }
        });*/
    }

    @Override //NodeListener
    public void onPeerDisconnected(final Node peer) {
        Log.v(TAG, "Peer Disconnected! ");
        // onPeerDisconnected:
        /*mHandler.post(new Runnable() {
            @Override
            public void run() {

            }
        });*/
    }

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }

        return results;
    }

    public void sendData(String[] boat_data) {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                    .addApi(Wearable.API)
                            // Optionally, add additional APIs and scopes if required.
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            Log.v("VORGSyncAdapter:", "New Connection on sendData!");
            mGoogleApiClient.connect();
        }
        if (mGoogleApiClient.isConnected()) {
            PutDataMapRequest dataMap = PutDataMapRequest.create(BD_PATH);
            dataMap.getDataMap().putStringArray(BD_KEY, boat_data);
            dataMap.getDataMap().putInt("count", count++);
            PutDataRequest request = dataMap.asPutDataRequest();
            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            Log.v(TAG, "Sending data was successful: " + String.valueOf(count)+" : "+ dataItemResult.getStatus()
                                    .isSuccess());
                        }
                    });
        }
    }


}
