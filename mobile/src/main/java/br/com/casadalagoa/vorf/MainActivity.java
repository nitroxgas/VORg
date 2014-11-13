package br.com.casadalagoa.vorf;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;

import br.com.casadalagoa.vorf.sync.VORSyncAdapter;


public class MainActivity extends FragmentActivity implements BoatFragment.Callback  {

   // private final String LOG_TAG = MainActivity.class.getSimpleName();

    //private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //mTwoPane = false;
        /*BoatFragment boatFragment =  ((BoatFragment)getSupportFragmentManager()
                .findFragmentById(R.id.fragment_boats));*/


        VORSyncAdapter.initializeSyncAdapter(this);
        VORSyncAdapter.syncImmediately(this, false);
    }

    @Override
    public void onItemSelected(String date) {

    }
/*
    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle("Next Update: "+Utility.getNextUpdate(getBaseContext()));
    }*/


    /*@Override
    public void onContentChanged() {
        super.onContentChanged();
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle("Next Update: "+Utility.getNextUpdate(getBaseContext()));
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            VORSyncAdapter.syncImmediately(this, false);
            return true;
        }
        if (id == R.id.action_about) {
            startActivity(new Intent(this, about_activity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
