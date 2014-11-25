package br.com.casadalagoa.vorf.conn;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import br.com.casadalagoa.vorf.sync.VORSyncAdapter;

public class VORgService  extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static VORSyncAdapter sVORSyncAdapter = null;

        @Override
        public void onCreate() {
            Log.d("VORgService", "onCreate");
            VORSyncAdapter.hasToSend = true;
            VORSyncAdapter.syncImmediately(getBaseContext(),false);
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
}
