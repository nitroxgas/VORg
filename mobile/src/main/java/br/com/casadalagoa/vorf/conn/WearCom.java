package br.com.casadalagoa.vorf.conn;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
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

import br.com.casadalagoa.vorf.Utility;
import br.com.casadalagoa.vorf.sync.VORSyncAdapter;


public class WearCom extends Activity implements  //DataApi.DataListener,
        MessageApi.MessageListener, NodeApi.NodeListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "WearCom";

    private static final String KEY_IN_RESOLUTION = "is_in_resolution";

    private static final String START_ACTIVITY_PATH = "/start-activity";

    // Patch and data keys to send to watch
    private static final String BD_PATH = "/bd"; // Boat Data
    private static final String BD_KEY = "bd"; // Boat data array!
    private static int count = 5;
    private Context mContext;


    private static final int REQUEST_RESOLVE_ERROR;

    static {
        REQUEST_RESOLVE_ERROR = 1000;
    }



    /**
     * Google API client.
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * Determines if the client is in a resolution state, and
     * waiting for resolution intent to return.
     */
    private boolean mIsInResolution;

    /**
     * Called when the activity is starting. Restores the activity state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mIsInResolution = savedInstanceState.getBoolean(KEY_IN_RESOLUTION, false);
        }
        mContext = getBaseContext();
    }


    /**
     * Called when the Activity is made visible.
     * A connection to Play Services need to be initiated as
     * soon as the activity is visible. Registers {@code ConnectionCallbacks}
     * and {@code OnConnectionFailedListener} on the
     * activities itself.
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                            // Optionally, add additional APIs and scopes if required.
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();
    }

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

    @Override //MessageListener
    public void onMessageReceived(final MessageEvent messageEvent) {
        Log.v(TAG, "Message Received!");
        if (messageEvent.getPath().equalsIgnoreCase(START_ACTIVITY_PATH)){
            VORSyncAdapter.syncImmediately(getBaseContext(),true);
        }
    }

    @Override //NodeListener
    public void onPeerConnected(final Node peer) {
        Log.v(TAG, "Peer Connected! ");
    }

    @Override //NodeListener
    public void onPeerDisconnected(final Node peer) {
        Log.v(TAG, "Peer Disconnected! ");
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
            dataMap.getDataMap().putString("next_event_title", Utility.getNextEventTitleInUse(getBaseContext()));
            dataMap.getDataMap().putString("next_event_time" , Utility.getNextEventTimeInUse(getBaseContext()));
            dataMap.getDataMap().putBoolean("show_countdown" , Utility.getNextEventShow(getBaseContext()));
            dataMap.getDataMap().putInt("counter", count++);  // Just to be certain that the ondatachanged will be called on the watch!
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
