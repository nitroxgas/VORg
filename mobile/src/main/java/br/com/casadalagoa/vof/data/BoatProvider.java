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
package br.com.casadalagoa.vof.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class BoatProvider extends ContentProvider {

    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private BoatDbHelper mOpenHelper;

    private static final int BOAT = 100;
    private static final int BOAT_WITH_CODE = 101;
    private static final int BOAT_WITH_CODE_AND_DATE = 102;
    private static final int CODE = 300;
    private static final int CODE_ID = 301;

    private static final SQLiteQueryBuilder sBoatByCodeSettingQueryBuilder;

    static{
        sBoatByCodeSettingQueryBuilder = new SQLiteQueryBuilder();
        sBoatByCodeSettingQueryBuilder.setTables(
                BoatContract.BoatEntry.TABLE_NAME + " INNER JOIN " +
                        BoatContract.CodeEntry.TABLE_NAME +
                        " ON " + BoatContract.BoatEntry.TABLE_NAME +
                        "." + BoatContract.BoatEntry.COLUMN_BOAT_ID +
                        " = " + BoatContract.CodeEntry.TABLE_NAME +
                        "." + BoatContract.CodeEntry.COLUMN_CODE);
    }

    private static final String sCodeSettingSelection =
            BoatContract.CodeEntry.TABLE_NAME+
                    "." + BoatContract.CodeEntry.COLUMN_CODE + " = ? ";

    private static final String sCodeSettingWithReportDateSelection =
            BoatContract.CodeEntry.TABLE_NAME+
                    "." + BoatContract.CodeEntry.COLUMN_CODE + " = ? AND " +
                    BoatContract.BoatEntry.COLUMN_REPORTDATE + " >= ? ";

    private static final String sCodeSettingAndDaySelection =
            BoatContract.CodeEntry.TABLE_NAME +
                    "." + BoatContract.CodeEntry.COLUMN_CODE + " = ? AND " +
                    BoatContract.BoatEntry.COLUMN_REPORTDATE + " = ? ";

    private Cursor getBoatByCodeSetting(Uri uri, String[] projection) {
        String codeSetting = BoatContract.BoatEntry.getCodeSettingFromUri(uri);
        String[] selectionArgs;
        String selection;
            selection = sCodeSettingSelection;
            selectionArgs = new String[]{codeSetting};
        return sBoatByCodeSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
    }

    private Cursor getBoatByCodeSettingAndDate(
            Uri uri, String[] projection, String sortOrder) {
        String codeSetting = BoatContract.BoatEntry.getCodeSettingFromUri(uri);
        return sBoatByCodeSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sCodeSettingSelection,
                new String[]{codeSetting},
                null,
                null,
                sortOrder
        );
    }

    private Cursor getBoatWithName(
            Uri uri, String[] projection, String sortOrder) {

        return sBoatByCodeSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                null,
                null,
                null,
                null,
                sortOrder
        );
    }

    private static UriMatcher buildUriMatcher() {
        // I know what you're thinking.  Why create a UriMatcher when you can use regular
        // expressions instead?  Because you're not crazy, that's why.

        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = BoatContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, BoatContract.PATH_BOAT, BOAT);
        matcher.addURI(authority, BoatContract.PATH_BOAT + "/*", BOAT_WITH_CODE);
        matcher.addURI(authority, BoatContract.PATH_BOAT + "/*/*", BOAT_WITH_CODE_AND_DATE);

        matcher.addURI(authority, BoatContract.PATH_CODE, CODE);
        matcher.addURI(authority, BoatContract.PATH_CODE + "/#", CODE_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new BoatDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                               String sortOrder) {
        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            // "Boat/*/*"
            case BOAT_WITH_CODE_AND_DATE:
            {
                retCursor = getBoatByCodeSettingAndDate(uri, projection, sortOrder);
                break;
            }
            // "Boat/*"
            case BOAT_WITH_CODE: {
                retCursor = getBoatByCodeSetting(uri, projection);
                break;
            }
            // "Boat"
            case BOAT: {
                retCursor = getBoatWithName(uri, projection, sortOrder);
                       /*mOpenHelper.getReadableDatabase().query(
                        BoatContract.BoatEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );*/
                break;
            }
            // "CODE/*"
            case CODE_ID: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        BoatContract.CodeEntry.TABLE_NAME,
                        projection,
                        BoatContract.CodeEntry._ID + " = '" + ContentUris.parseId(uri) + "'",
                        null,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // "CODE"
            case CODE: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        BoatContract.CodeEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public String getType(Uri uri) {

        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case BOAT_WITH_CODE_AND_DATE:
                return BoatContract.BoatEntry.CONTENT_ITEM_TYPE;
            case BOAT_WITH_CODE:
                return BoatContract.BoatEntry.CONTENT_TYPE;
            case BOAT:
                return BoatContract.BoatEntry.CONTENT_TYPE;
            case CODE:
                return BoatContract.CodeEntry.CONTENT_TYPE;
            case CODE_ID:
                return BoatContract.CodeEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case BOAT: {
                long _id = db.insert(BoatContract.BoatEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = BoatContract.BoatEntry.buildBoatUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case CODE: {
                long _id = db.insert(BoatContract.CodeEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = BoatContract.CodeEntry.buildCodeUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        switch (match) {
            case BOAT:
                rowsDeleted = db.delete(
                        BoatContract.BoatEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case CODE:
                rowsDeleted = db.delete(
                        BoatContract.CodeEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (selection == null || rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(
            Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case BOAT:
                rowsUpdated = db.update(BoatContract.BoatEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            case CODE:
                rowsUpdated = db.update(BoatContract.CodeEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case BOAT:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(BoatContract.BoatEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }
}
