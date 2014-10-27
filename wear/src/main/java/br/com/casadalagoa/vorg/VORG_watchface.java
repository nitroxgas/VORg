package br.com.casadalagoa.vorg;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;


public class VORG_watchface extends WatchFaceActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener, MessageApi.MessageListener,
        NodeApi.NodeListener {

    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";
    public static final String COUNT_PATH = "/count";
    public static final String IMAGE_PATH = "/image";
    public static final String IMAGE_KEY = "photo";
    private static final String COUNT_KEY = "count";
    private static final int MAX_LOG_TAG_LENGTH = 23;

    // VOR Boat Info
    public static final String SPEED_PATH = "/speed";
    public static final String SPEED_KEY = "speed";
    public static final String WSPEED_PATH = "/wspeed";
    public static final String WSPEED_KEY = "wspeed";
    public static final String WDGREE_PATH = "/wdgree";
    public static final String WDGREE_KEY = "wdgree";
    public static final String TWA_PATH = "/twa";
    public static final String TWA_KEY = "twa";
    public static final String RANK_PATH = "/rank";
    public static final String RANK_KEY = "rank";

    private static final String TAG = "VORG_watchface";

    private GoogleApiClient mGoogleApiClient;
    private ListView mDataItemList;
    private TextView mIntroText;
   // private DataItemAdapter mDataItemListAdapter;
    private View mLayout;
    private Handler mHandler;

    private TextView mTextView;
    private TextView mTime, mBattery, mTWA, mWAngle, mWSpeed, mSpeed, mRanking;
    private int mDataRec;

    private final static IntentFilter INTENT_FILTER;
    static {
        INTENT_FILTER = new IntentFilter();
        INTENT_FILTER.addAction(Intent.ACTION_TIME_TICK);
        INTENT_FILTER.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        INTENT_FILTER.addAction(Intent.ACTION_TIME_CHANGED);
    }

    private final String TIME_FORMAT_DISPLAYED = "HH:mm";

    private BroadcastReceiver mTimeInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context arg0, Intent intent) {
            if (mTime!=null) {
                mTime.setText(
                        new SimpleDateFormat(TIME_FORMAT_DISPLAYED)
                                .format(Calendar.getInstance().getTime()));
                mSpeed.setText(String.valueOf(mDataRec));
                /*
                mWAngle.setText("--");
                mWSpeed.setText("--");
                mRanking.setText("--");
                */
            }
        }
    };

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context arg0, Intent intent) {
           if (mBattery!=null) mBattery.setText(String.valueOf(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) + "%"));
        }
    };

    @Override
    public void onScreenDim() {
        /*mTime.setTextColor(Color.WHITE);
        mBattery.setTextColor(Color.WHITE);*/
    }

    @Override
    public void onScreenAwake() {
//        mTime.setTextColor(Color.RED);
//        mBattery.setTextColor(Color.RED);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vorg_watchface);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mBattery = (TextView) stub.findViewById(R.id.watch_battery);
                mTime = (TextView) stub.findViewById(R.id.watch_time);
                mRanking = (TextView) stub.findViewById(R.id.mRank);
                mSpeed = (TextView) stub.findViewById(R.id.mWSpeed);
                mTWA = (TextView) stub.findViewById(R.id.mTWA);
                mWSpeed = (TextView) stub.findViewById(R.id.mBSpeed);
                mWAngle = (TextView) stub.findViewById(R.id.mWindDegree);
                mTimeInfoReceiver.onReceive(VORG_watchface.this, registerReceiver(null, INTENT_FILTER));
                mLayout = findViewById(R.id.lay_rel_inc);
                mDataRec = 0;

                  //  Here, we're just calling our onReceive() so it can set the current time.
                registerReceiver(mTimeInfoReceiver, INTENT_FILTER);
                registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            }
        });
        mHandler = new Handler();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //unregisterReceiver(mTimeInfoReceiver);
        //unregisterReceiver(mBatInfoReceiver);
        //Wearable.DataApi.removeListener(mGoogleApiClient, this);
        //Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        //Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        //if (mRanking!=null) mRanking.setText("Pause");
     //   mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        LOGD(TAG, "onConnected(): Successfully connected to Google API client");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.NodeApi.addListener(mGoogleApiClient, this);
        if (mRanking!=null)  mRanking.setText("C");
    }


    @Override
    protected void onResume() {
        super.onResume();
        //mGoogleApiClient.connect();
        /*if (mTWA != null){
            mTWA.setText("Resume");
        } else   LOGD(TAG,"Resume");*/
        //registerReceiver(mTimeInfoReceiver, INTENT_FILTER);
        //registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }


    @Override
    public void onConnectionSuspended(int cause) {
        LOGD(TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
        if (mRanking!=null) mRanking.setText("onCSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "onConnectionFailed(): Failed to connect, with result: " + result);
        if (mRanking!=null) mRanking.setText("onCFail");
    }

    private void generateEvent(final String title, final String text) {
        LOGD(TAG, "NEV:" + title);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
             if (mTWA != null) {
                mTWA.setText(text);
              } else LOGD(TAG, "EV");
                // mIntroText.setVisibility(View.INVISIBLE);
                // mDataItemListAdapter.add(new Event(title, text));
             }
        });
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        LOGD(TAG, "onDataChanged(): " + dataEvents);
        //if (mSpeed!=null) mSpeed.setText(String.valueOf(mDataRec));
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();
        for (DataEvent event : events) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                if (IMAGE_PATH.equals(path)) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    Asset photo = dataMapItem.getDataMap()
                            .getAsset(IMAGE_KEY);
                    final Bitmap bitmap = loadBitmapFromAsset(mGoogleApiClient, photo);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "Setting background image..");
                            mLayout.setBackground(new BitmapDrawable(getResources(), bitmap));
                        }
                    });

                } else if (COUNT_PATH.equals(path)) {
                    LOGD(TAG, "Data Changed for COUNT_PATH");
                    mDataRec++;
                    generateEvent("DataItem Changed", event.getDataItem().getData().toString());
                } else {
                    LOGD(TAG, "Unrecognized path: " + path);
                }

            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                //generateEvent("DataItem Deleted", event.getDataItem().toString());
            } else {
                //generateEvent("Unknown data event type", "Type = " + event.getType());
            }
        }
    }

    /**
     * Extracts {@link android.graphics.Bitmap} data from the
     * {@link com.google.android.gms.wearable.Asset}
     */
    private Bitmap loadBitmapFromAsset(GoogleApiClient apiClient, Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(apiClient, asset).await().getInputStream();
        if (assetInputStream == null) {
            Log.w(TAG, "Requested an unknown Asset.");
            return null;
        }
        return BitmapFactory.decodeStream(assetInputStream);
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        LOGD(TAG, "onMessageReceived: " + event);
        mTWA.setText("Received");
       generateEvent("Message", event.toString());
    }

    @Override
    public void onPeerConnected(Node node) {
        generateEvent("Node Connected", node.getId());
    }

    @Override
    public void onPeerDisconnected(Node node) {
        generateEvent("Node Disconnected", node.getId());
    }


    private class Event {
        String title;
        String text;

        public Event(String title, String text) {
            this.title = title;
            this.text = text;
        }
    }


    public static void LOGD(final String tag, String message) {
        if (Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, message);
        }
        Log.d(tag, message);
    }


}
