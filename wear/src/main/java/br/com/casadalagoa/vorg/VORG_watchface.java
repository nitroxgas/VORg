package br.com.casadalagoa.vorg;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;


public class VORG_watchface extends WatchFaceActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener, MessageApi.MessageListener,
        NodeApi.NodeListener {

    // Patch and data keys to send to watch
    private static final String BD_PATH = "/bd"; // Boat Data
    private static final String BD_KEY = "bd"; // Boat data array!

    private static final String TAG = "VOR_watchface";

    private GoogleApiClient mGoogleApiClient;
   //private View mLayout;

    //private Handler mHandler;

    private TextView mTime, mBattery, mTWA, mWAngle, mWSpeed, mSpeed, mRanking, mLocale, mLegc, mDTL, mCenter;
    private ImageView mImg;
    //private int mDataRec;  // Count received data

    private final static IntentFilter INTENT_FILTER;
    static {
        INTENT_FILTER = new IntentFilter();
        INTENT_FILTER.addAction(Intent.ACTION_TIME_TICK);
        INTENT_FILTER.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        INTENT_FILTER.addAction(Intent.ACTION_TIME_CHANGED);
    }

    private BroadcastReceiver mTimeInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context arg0, Intent intent) {
            if (mTime!=null) {
                String TIME_FORMAT_DISPLAYED = "HH:mm";
                mTime.setText(
                        new SimpleDateFormat(TIME_FORMAT_DISPLAYED)
                                .format(Calendar.getInstance().getTime()));
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
                mLocale = (TextView) stub.findViewById(R.id.mLocal);
                mDTL = (TextView) stub.findViewById(R.id.mDTLC);
                mLegc = (TextView) stub.findViewById(R.id.mLC);
                mCenter = (TextView) stub.findViewById(R.id.center_txt);
                mImg = (ImageView) stub.findViewById(R.id.img_boat);
                mTimeInfoReceiver.onReceive(VORG_watchface.this, registerReceiver(null, INTENT_FILTER));
               // mLayout = findViewById(R.id.lay_rel_inc);
                mBattery.setText(""); mTime.setText(""); mRanking.setText("");
                mSpeed.setText(""); mTWA.setText(""); mWSpeed.setText(""); mWAngle.setText("");
                mLocale.setText(""); mDTL.setText(""); mLegc.setText("");

                String TIME_FORMAT_DISPLAYED = "HH:mm";
                mTime.setText(
                        new SimpleDateFormat(TIME_FORMAT_DISPLAYED)
                                .format(Calendar.getInstance().getTime()));

                  //  Here, we're just calling our onReceive() so it can set the current time.

                registerReceiver(mTimeInfoReceiver, INTENT_FILTER);
                registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            }
        });
        //mHandler = new Handler();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
        LOGD(TAG, "onCreated()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LOGD(TAG, "onDestroy()");
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
        unregisterReceiver(mTimeInfoReceiver);
        unregisterReceiver(mBatInfoReceiver);

    }

    @Override
    protected void onPause() {
        super.onPause();
        LOGD(TAG, "onPause()");
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        LOGD(TAG, "onConnected(): Successfully connected to Google API client");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.NodeApi.addListener(mGoogleApiClient, this);
       // if (mRanking!=null)  mRanking.setText("C");
    }


    @Override
    protected void onResume() {
        super.onResume();
        LOGD(TAG, "onResume()");
  }


    @Override
    public void onConnectionSuspended(int cause) {
        LOGD(TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "onConnectionFailed(): Failed to connect, with result: " + result);
    }

    private void generateEvent(final String title, final String text) {
        LOGD(TAG, "NEV:" + title + text);
       /* runOnUiThread(new Runnable() {
            @Override
            public void run() {
              LOGD(TAG, "EV:"+text);
                // mIntroText.setVisibility(View.INVISIBLE);
                // mDataItemListAdapter.add(new Event(title, text));
             }
        });*/
    }

    private void updateUI(final String[] boat_data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mTWA != null) {
                    mTWA.setText(boat_data[13]);
                    mLocale.setText(boat_data[5]+"\n"+boat_data[6]);
                    mRanking.setText(boat_data[9]);
                    mSpeed.setText(boat_data[16]);
                    mWSpeed.setText(boat_data[17]);
                    mWAngle.setText(boat_data[19]);
                    mLegc.setText("L1 "+boat_data[11]+"%");
                    /*Long lDtlc = Long.getLong(boat_data[7]);
                    int iDtl = Integer.getInteger(boat_data[12]);*/
                    mDTL.setText("DTLC "+boat_data[7]+"\nDTL "+boat_data[12]);
                    mImg.setImageResource(Utility.getFormattedBoatHeading(getApplicationContext(),boat_data[23],boat_data[13]));
                    mImg.setVisibility(View.VISIBLE);
                    mCenter.setText("");
                }

            }
        });
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        LOGD(TAG, "onDataChanged(): " + dataEvents);
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();
        for (DataEvent event : events) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                if (BD_PATH.equals(path)) {
                    LOGD(TAG, "Boat Data Changed...");
                    //mDataRec++;
                    //generateEvent("DataItem Changed", Arrays.toString(event.getDataItem().getData()));
                    DataMapItem dataItem = DataMapItem.fromDataItem (event.getDataItem());
                    String[] boat_data = dataItem.getDataMap().getStringArray(BD_KEY);
                   /* String tmp_str=String.valueOf(boat_data.length);
                    for (int i=0;i<boat_data.length;i++){
                        tmp_str+="("+boat_data[i]+")";
                    }
                    Log.v(TAG, "DATA["+tmp_str+"]");
                             //  0    1     2          3             4      5        6         7    8   9             10               11        12    13             14   15             16             17               18              19                  20                  21        22  23  24    25
                            //[_id,b_code,reportdate,timeoffixdate,status,latitude,longitude,dtf,dtlc,legstanding,twentyfourhourrun,legprogress,dul,boatheadingtrue,smg,seatemperature,truwindspeedavg,speedthrowater,truewindspeedmax,truewinddirection,latestspeedthrowater,maxavgspeed,_id,code,name,color]
                                 0    1       2                     3                 4      5        6         7          8     9   10  11   12       13   14    15   16  17  18   19  20  21   22   23    24           25
                        //DATA[(818)(TBRU)(2014-11-02 12:40:00)(2014-11-02 12:40:00)(RAC)(-6.68983)(-40.64467)(01258.70)(-00001)(3)(324)(81)(00023.80)(99)(19.8)(null)(21)(20)(21)(330)(20)(20)(818)(TBRU)(Team Brunel)(d3dc3c)]

                    */
                    updateUI(boat_data);
                } else {
                    LOGD(TAG, "Unrecognized path: " + path);
                }

            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                generateEvent("DataItem Deleted", event.getDataItem().toString());
            } else {
                generateEvent("Unknown data event type", "Type = " + event.getType());
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        LOGD(TAG, "onMessageReceived: " + event);
       // mTWA.setText("Received");
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

    public static void LOGD(final String tag, String message) {
        if (Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, message);
        }
        Log.v(tag, message);
    }


}
