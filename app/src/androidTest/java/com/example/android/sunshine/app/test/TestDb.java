package com.example.android.sunshine.app.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import android.util.Log;

import java.util.Map;
import java.util.Set;

import app.sunshine.android.example.com.sunshine.data.WeatherContract.LocationEntry;
import app.sunshine.android.example.com.sunshine.data.WeatherContract.WeatherEntry;
import app.sunshine.android.example.com.sunshine.data.WeatherDbHelper;

/**
 * Created by Araceli on 13/08/2014.
 */
public class TestDb extends AndroidTestCase {

    public static final String LOG_TAG = TestDb.class.getSimpleName();

    // Test to create the DB
    public void testCreateDb() throws Throwable {
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
        SQLiteDatabase db = new WeatherDbHelper(this.mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());
        db.close();
    }

    public void testInsertReadDb() {

        WeatherDbHelper dbHelper = new WeatherDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Location data
        ContentValues values = getLocationContentValues();

        long locationRowId = db.insert(LocationEntry.TABLE_NAME, null, values);

        // Did we get a row back?
        assertTrue(locationRowId != -1);
        Log.d(LOG_TAG, "New row id: " + locationRowId);

        // query results
        Cursor cursor = db.query(
                LocationEntry.TABLE_NAME,  // Table to Query
                null, // columns
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null // sort order
        );

        validateCursor(values, cursor);

        /*
        if (cursor.moveToFirst()) {
            int settingIndex = cursor.getColumnIndex(LocationEntry.COLUMN_LOCATION_SETTING);
            String location = cursor.getString(settingIndex);

            int nameIndex = cursor.getColumnIndex(LocationEntry.COLUMN_CITY_NAME);
            String name = cursor.getString(nameIndex);

            int latitudeIndex = cursor.getColumnIndex(LocationEntry.COLUMN_LATITUDE);
            double latitude = cursor.getDouble(latitudeIndex);

            int longitudeIndex = cursor.getColumnIndex(LocationEntry.COLUMN_LONGITUDE);
            double longitude = cursor.getDouble(longitudeIndex);

            assertEquals(location, testLocationSetting);
            assertEquals(name, testName);
            assertEquals(latitude, testLatitude);
            assertEquals(longitude, testLongitude);
        } else {
            fail("No location values");
        }
        */

        // Weather data
        ContentValues weatherValues = getWeatherContentValues(locationRowId);

        long weatherRowId = db.insert(WeatherEntry.TABLE_NAME, null, weatherValues);
        assertTrue(weatherRowId != -1);

        Cursor weatherCursor = db.query
                (WeatherEntry.TABLE_NAME, null, null, null, null, null, null);

        validateCursor(weatherValues, weatherCursor);

        dbHelper.close();

        /*
        if (weatherCursor.moveToFirst()) {
            int dateIndex = weatherCursor.getColumnIndex(WeatherEntry.COLUMN_DATETEXT);
            String date = weatherCursor.getString(dateIndex);

            int degreeIndex = weatherCursor.getColumnIndex(WeatherEntry.COLUMN_DEGREES);
            Double degree = weatherCursor.getDouble(degreeIndex);

            int maxIndex = weatherCursor.getColumnIndex(WeatherEntry.COLUMN_MAX_TEMP);
            int max = weatherCursor.getInt(maxIndex);

            int minIndex = weatherCursor.getColumnIndex(WeatherEntry.COLUMN_MIN_TEMP);
            int min = weatherCursor.getInt(minIndex);

            assertEquals(date, "20141205");
            assertEquals(degree, 1.1);
            assertEquals(max, 75);
            assertEquals(min, 65);

        } else {
            fail("No weather values");
        }
        */

    }

    static ContentValues getLocationContentValues() {
        ContentValues values = new ContentValues();

        String testName = "North Pole";
        String testLocationSetting = "99705";
        double testLatitude = 64.772;
        double testLongitude = -147.355;

        values.put(LocationEntry.COLUMN_CITY_NAME, testName);
        values.put(LocationEntry.COLUMN_LOCATION_SETTING, testLocationSetting);
        values.put(LocationEntry.COLUMN_LATITUDE, testLatitude);
        values.put(LocationEntry.COLUMN_LONGITUDE, testLongitude);
        return values;
    }

    static ContentValues getWeatherContentValues(long locationRowId) {
        ContentValues values = new ContentValues();

        values.put(WeatherEntry.COLUMN_LOC_KEY, locationRowId);
        values.put(WeatherEntry.COLUMN_DATETEXT, "20141205");
        values.put(WeatherEntry.COLUMN_DEGREES, 1.1);
        values.put(WeatherEntry.COLUMN_HUMIDITY, 1.2);
        values.put(WeatherEntry.COLUMN_PRESSURE, 1.3);
        values.put(WeatherEntry.COLUMN_MAX_TEMP, 75);
        values.put(WeatherEntry.COLUMN_MIN_TEMP, 65);
        values.put(WeatherEntry.COLUMN_SHORT_DESC, "Asteroids");
        values.put(WeatherEntry.COLUMN_WIND_SPEED, 5.5);
        values.put(WeatherEntry.COLUMN_WEATHER_ID, 321);
        return values;
    }

    // Validate data (refactored version)
    static public void validateCursor(ContentValues contentValues, Cursor cursor) {

        assertTrue(cursor.moveToFirst());

        Set<Map.Entry<String, Object>> valueSet = contentValues.valueSet();

        for(Map.Entry<String, Object> value : valueSet) {
            String columnName = value.getKey();
            int index = cursor.getColumnIndex(columnName);
            assertFalse(-1 == index);
            String expectedValue = value.getValue().toString();
            assertEquals(expectedValue, cursor.getString(index));
        }
        cursor.close();
    }


}
