package br.com.casadalagoa.vorf.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class VORSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static VORSyncAdapter sVORSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d("VORSyncService", "onCreate - VORSyncService");
        synchronized (sSyncAdapterLock) {
            if (sVORSyncAdapter == null) {
                sVORSyncAdapter = new VORSyncAdapter(getApplicationContext(), false);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sVORSyncAdapter.getSyncAdapterBinder();
    }
}
