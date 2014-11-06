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

import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import br.com.casadalagoa.vof.data.BoatContract;
import br.com.casadalagoa.vof.data.BoatContract.BoatEntry;
import br.com.casadalagoa.vof.sync.VORSyncAdapter;

/**
 * Encapsulates fetching the forecast and displaying it as a {@link android.widget.ListView} layout.
 */
public class BoatFragment extends Fragment implements LoaderCallbacks<Cursor> {

    private BoatAdapter mBoatAdapter;

    private String mCode;
    private ListView mListView;
    private int mPosition = ListView.INVALID_POSITION;
   //private boolean mUseTodayLayout;

    private static final String SELECTED_KEY = "selected_position";
    //private static final String SELECTED_BOAT = "selected_boat";

    private static final int BOAT_LOADER = 0;

    // For the forecast view we're showing only a small subset of the stored data.
    // Specify the columns we need.
    private static final String[] BOAT_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            BoatEntry.TABLE_NAME + "." + BoatEntry._ID,
            BoatEntry.COLUMN_LEG_STANDING,
            BoatEntry.COLUMN_SPEEDTHROWATER,
            BoatEntry.COLUMN_TRUEWINDSPEEDMAX,
            BoatEntry.COLUMN_BOATHEADINGTRUE,
            BoatEntry.COLUMN_LEGPROGRESS,
            BoatEntry.COLUMN_BOAT_ID,
            BoatContract.CodeEntry.TABLE_NAME+"."+ BoatContract.CodeEntry.COLUMN_NAME,
            BoatContract.CodeEntry.TABLE_NAME+"."+ BoatContract.CodeEntry.COLUMN_COLOR,
            BoatEntry.COLUMN_DUL
    };


    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    public static final int COL_BOAT_ID = 0;
    public static final int COL_BOAT_LEGPROGRESS = 5;
    public static final int COL_BOAT_LEGSTANDING = 1;
    public static final int COL_BOAT_SPEEDTHROUGWATER = 2;
    public static final int COL_BOAT_TRUEWINDSPEEDMAX = 3;
    public static final int COL_CODE_NAME = 7;
    public static final int COL_BOAT_BOATHEADINGTRUE = 4;
    public static final int COL_CODE_COLOR = 8;
    public static final int COL_BOAT_CODE = 6;
    //public static final int COL_BOAT_DUL = 9;
                   //        0            1            2              3                   4             5           6       7           8
    //SQLiteQuery: SELECT boats._id, legstanding, speedthrowater, truewindspeedmax, boatheadingtrue, legprogress, b_code, codes.name, codes.color FROM boats INNER JOIN codes ON boats.b_code = codes.code ORDER BY legstanding ASC


    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(String date);
    }

    public BoatFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

  /*  @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
    }
*/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // The ArrayAdapter will take data from a source and
        // use it to populate the ListView it's attached to.
        mBoatAdapter = new BoatAdapter(getActivity(), null, 0);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        mListView = (ListView) rootView.findViewById(R.id.listview_boats);
        mListView.setAdapter(mBoatAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Cursor cursor = mBoatAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    ((Callback) getActivity())
                            .onItemSelected(cursor.getString(COL_BOAT_ID));
                    Context mContext = getActivity().getBaseContext();
                    Utility.setPreferredBoat(mContext, cursor.getString(COL_BOAT_CODE));
                    updateBoat();
                }
                mPosition = position;
            }
        });

        // If there's instance state, mine it for useful information.
        // The end-goal here is that the user never knows that turning their device sideways
        // does crazy lifecycle related things.  It should feel like some stuff stretched out,
        // or magically appeared to take advantage of room, but data or place in the app was never
        // actually *lost*.
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            // The listview probably hasn't even been populated yet.  Actually perform the
            // swapout in onLoadFinished.
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        //mBoatAdapter.setUseTodayLayout(mUseTodayLayout);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(BOAT_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    private void updateBoat() {
        VORSyncAdapter.syncImmediately(getActivity(), true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCode != null && !mCode.equals(Utility.getPreferredBoat(getActivity()))) {
            getLoaderManager().restartLoader(BOAT_LOADER, null, this);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // When tablets rotate, the currently selected list item needs to be saved.
        // When no item is selected, mPosition will be set to Listview.INVALID_POSITION,
        // so check for that before storing.
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // fragment only uses one loader, so we don't care about checking the id.

        // To only show current and future dates, get the String representation for today,
        // and filter the query to return weather only for dates after or including today.
        // Only return data after today.
        //String startDate = BoatContract.getDbDateString(new Date());

        // Sort order:  Ascending, by date.
        String sortOrder = BoatEntry.COLUMN_LEG_STANDING + " ASC";

        mCode = Utility.getPreferredBoat(getActivity());
        Uri standingUri = BoatEntry.buildBoatUri(1);//  buildBoatCode(mCode);

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(
                getActivity(),
                standingUri,
                BOAT_COLUMNS,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mBoatAdapter.swapCursor(data);
        if (mPosition != ListView.INVALID_POSITION) {
            // If we don't need to restart the loader, and there's a desired position to restore
            // to, do so now.
            mListView.smoothScrollToPosition(mPosition);
        }
        try {
            Context context = getActivity().getBaseContext();

            View rootView = getView();
            TextView nextUpdateView;
            nextUpdateView = (TextView) rootView.findViewById(R.id.nextUpdateView);
            if (nextUpdateView != null) {
                nextUpdateView.setText("Next Update: " + Utility.getNextUpdate(context));
            }
        } catch (NullPointerException e){
            Log.e("BoatFragment:", "No view to show update");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mBoatAdapter.swapCursor(null);
    }

   /* public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = false;//useTodayLayout;
        if (mBoatAdapter != null) {
            mBoatAdapter.setUseTodayLayout(mUseTodayLayout);
        }
    }*/

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}
