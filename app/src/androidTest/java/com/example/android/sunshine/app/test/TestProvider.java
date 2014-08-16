package com.example.android.sunshine.app.test;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.util.Log;

import app.sunshine.android.example.com.sunshine.data.WeatherContract.LocationEntry;
import app.sunshine.android.example.com.sunshine.data.WeatherContract.WeatherEntry;
import app.sunshine.android.example.com.sunshine.data.WeatherDbHelper;

/**
 * Created by Araceli on 13/08/2014.
 */
public class TestProvider extends AndroidTestCase {

    public static final String LOG_TAG = TestProvider.class.getSimpleName();

    static public String TEST_LOCATION = "99705";
    static public String TEST_DATE = "20141205";

    // Test to create the DB
    public void testDeleteDb() throws Throwable {
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
    }

    public void testInsertReadProvider() {
        // Use the weather provider to get the weather query

        WeatherDbHelper dbHelper = new WeatherDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Location data
        ContentValues values = TestDb.getLocationContentValues();

        // Insert location data
        Uri locationInsertUri = mContext.getContentResolver().insert(LocationEntry.CONTENT_URI, values);
        long locationRowId = ContentUris.parseId(locationInsertUri);

        // Verify we got a row back
        assertTrue(locationRowId != -1);
        Log.d(LOG_TAG, "New row id: " + locationRowId);

        // query results
        Cursor cursor = mContext.getContentResolver().query(
                LocationEntry.CONTENT_URI,  // Table to Query
                null, // columns
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null // sort order
        );

        TestDb.validateCursor(values, cursor);

        // query results including row id
        cursor = mContext.getContentResolver().query(
                LocationEntry.buildLocationUri(locationRowId),  // Table to Query
                null, // columns
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null // sort order
        );

        TestDb.validateCursor(values, cursor);
        Log.d(LOG_TAG, "validating id:"+locationRowId);



        // Weather data
        ContentValues weatherValues = TestDb.getWeatherContentValues(locationRowId);

        // Insert weather data (via content provider)
        Uri insertUri = mContext.getContentResolver().insert(WeatherEntry.CONTENT_URI, weatherValues);
        long weatherRowId = ContentUris.parseId(insertUri);
        assertTrue(insertUri != null);

        // query results
        Cursor weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.CONTENT_URI,
                null, // columns
                null, // cols for "where" clause
                null, // values for "where" clause
                null  //sort order
        );

        TestDb.validateCursor(weatherValues, weatherCursor);

        weatherCursor.close();

        weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.buildWeatherLocation(TEST_LOCATION),
                null, // columns
                null, // cols for "where" clause
                null, // values for "where" clause
                null  //sort order
        );

        if(weatherCursor.moveToFirst()) {
            TestDb.validateCursor(weatherValues, weatherCursor);
        } else {
            fail("No weather data returned");
        }

        weatherCursor.close();

        weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.buildWeatherLocationWithStartDate(TEST_LOCATION, TEST_DATE),
                null, // columns
                null, // cols for "where" clause
                null, // values for "where" clause
                null  //sort order
        );

        TestDb.validateCursor(weatherValues, weatherCursor);

        weatherCursor.close();

        weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.buildWeatherLocationWithDate(TEST_LOCATION, TEST_DATE),
                null, // columns
                null, // cols for "where" clause
                null, // values for "where" clause
                null  //sort order
        );

        TestDb.validateCursor(weatherValues, weatherCursor);



        dbHelper.close();

    }

    public void testGetType() {
        // content://com.example.android.sunshine.app/weather/
        String type = mContext.getContentResolver().getType(WeatherEntry.CONTENT_URI);
        // vnd.android.cursor.dir/com.example.android.sunshine.app/weather
        assertEquals(WeatherEntry.CONTENT_TYPE, type);

        String testLocation = "94074";
        // content://com.example.android.sunshine.app/weather/94074
        type = mContext.getContentResolver().getType(
                WeatherEntry.buildWeatherLocation(testLocation));
        // vnd.android.cursor.dir/com.example.android.sunshine.app/weather
        assertEquals(WeatherEntry.CONTENT_TYPE, type);

        String testDate = "20140612";
        // content://com.example.android.sunshine.app/weather/94074/20140612
        type = mContext.getContentResolver().getType(
                WeatherEntry.buildWeatherLocationWithDate(testLocation, testDate));
        // vnd.android.cursor.item/com.example.android.sunshine.app/weather
        assertEquals(WeatherEntry.CONTENT_ITEM_TYPE, type);

        // content://com.example.android.sunshine.app/location/
        type = mContext.getContentResolver().getType(LocationEntry.CONTENT_URI);
        // vnd.android.cursor.dir/com.example.android.sunshine.app/location
        assertEquals(LocationEntry.CONTENT_TYPE, type);

        // content://com.example.android.sunshine.app/location/1
        type = mContext.getContentResolver().getType(LocationEntry.buildLocationUri(1L));
        // vnd.android.cursor.item/com.example.android.sunshine.app/location
        assertEquals(LocationEntry.CONTENT_ITEM_TYPE, type);
    }


}
