package br.com.casadalagoa.vorg;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;

import br.com.casadalagoa.vorg.sync.VORSyncAdapter;


public class MainActivity extends FragmentActivity implements BoatFragment.Callback  {

   // private final String LOG_TAG = MainActivity.class.getSimpleName();

    //private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //mTwoPane = false;
        BoatFragment boatFragment =  ((BoatFragment)getSupportFragmentManager()
                .findFragmentById(R.id.fragment_boats));
        boatFragment.setUseTodayLayout(false);

        VORSyncAdapter.initializeSyncAdapter(this);
        //Utility.getBoatArray(this.getBaseContext(), this.getBaseContext().getString(R.string.pref_boat_key));
        VORSyncAdapter.syncImmediately(this, false);
    }

    @Override
    public void onItemSelected(String date) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /*@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            VORSyncAdapter.syncImmediately(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/
}
