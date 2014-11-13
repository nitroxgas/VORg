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
package br.com.casadalagoa.vorf.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import br.com.casadalagoa.vorf.data.BoatContract.BoatEntry;
import br.com.casadalagoa.vorf.data.BoatContract.CodeEntry;

/**
 * Manages a local database for boat data.
 */
public class BoatDbHelper extends SQLiteOpenHelper {

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 8;

    public static final String DATABASE_NAME = "vor_boat.db";

    public BoatDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        //sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + CodeEntry.TABLE_NAME);

        final String SQL_CREATE_CODE_TABLE = "CREATE TABLE " + CodeEntry.TABLE_NAME + " (" +
                CodeEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                CodeEntry.COLUMN_CODE + " TEXT UNIQUE NOT NULL, " +
                CodeEntry.COLUMN_NAME + " TEXT NOT NULL, " +
                CodeEntry.COLUMN_COLOR + " TEXT NOT NULL, " +
                "UNIQUE (" + CodeEntry.COLUMN_CODE +") ON CONFLICT REPLACE"+
                " );";

        final String SQL_CREATE_BOAT_TABLE = "CREATE TABLE " + BoatEntry.TABLE_NAME + " (" +
                // Why AutoIncrement here, and not above?
                // Unique keys will be auto-generated in either case.  But for boat
                // entry, it's reasonable to assume the user will want information
                // for a certain date and all dates *following*
                BoatEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                // the ID of the boat entry associated with this data
                BoatEntry.COLUMN_BOAT_ID + " TEXT NOT NULL, " +
                BoatEntry.COLUMN_REPORTDATE + " TEXT NOT NULL, " +
                BoatEntry.COLUMN_TIMEOFFIX + " TEXT NOT NULL, " +
                BoatEntry.COLUMN_STATUS + " TEXT NOT NULL, " +
                BoatEntry.COLUMN_LATITUDE + " TEXT NOT NULL, " +
                BoatEntry.COLUMN_LONGITUDE + " TEXT NOT NULL, " +
                BoatEntry.COLUMN_DTF + " TEXT NOT NULL, " +
                BoatEntry.COLUMN_DTLC + " TEXT NOT NULL, " +
                BoatEntry.COLUMN_LEG_STANDING + " TEXT NOT NULL, " +
                BoatEntry.COLUMN_TWENTYFOURHOURRUN + " TEXT NOT NULL, " +
                BoatEntry.COLUMN_LEGPROGRESS + " TEXT NOT NULL, " +
                BoatEntry.COLUMN_DUL + " TEXT NOT NULL, " +
                BoatEntry.COLUMN_BOATHEADINGTRUE + " TEXT NOT NULL, " +
                BoatEntry.COLUMN_SMG + " TEXT NOT NULL, " +
                BoatEntry.COLUMN_SEATEMPERATURE + " TEXT NOT NULL, " +
                BoatEntry.COLUMN_TRUWINDSPEEDAVG + " TEXT NOT NULL, " +
                BoatEntry.COLUMN_SPEEDTHROWATER + " TEXT NOT NULL, " +
                BoatEntry.COLUMN_TRUEWINDSPEEDMAX + " TEXT NOT NULL, " +
                BoatEntry.COLUMN_TRUEWINDDIRECTION + " TEXT NOT NULL, " +
                BoatEntry.COLUMN_LATESTSPEEDTHROWATER + " TEXT NOT NULL, " +
                BoatEntry.COLUMN_MAXAVGSPEED + " TEXT NOT NULL, " +
                // Set up the location column as a foreign key to location table.
                " FOREIGN KEY (" + BoatEntry.COLUMN_BOAT_ID + ") REFERENCES " +
                CodeEntry.TABLE_NAME + " (" + BoatEntry.COLUMN_BOAT_ID + "), " +

                // To assure the application have just one weather entry per day
                // per location, it's created a UNIQUE constraint with REPLACE strategy
                " UNIQUE (" +
                BoatEntry.COLUMN_BOAT_ID + ") ON CONFLICT REPLACE);";

        sqLiteDatabase.execSQL(SQL_CREATE_CODE_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_BOAT_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        // Note that this only fires if you change the version number for your database.
        // It does NOT depend on the version number for your application.
        // If you want to update the schema without wiping data, commenting out the next 2 lines
        // should be your top priority before modifying this method.
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + CodeEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + BoatEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
