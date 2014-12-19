package app.sunshine.android.example.com.sunshine.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import app.sunshine.android.example.com.sunshine.MainActivity;
import app.sunshine.android.example.com.sunshine.R;
import app.sunshine.android.example.com.sunshine.Utility;
import app.sunshine.android.example.com.sunshine.data.WeatherContract;

import static app.sunshine.android.example.com.sunshine.data.WeatherContract.LocationEntry;
import static app.sunshine.android.example.com.sunshine.data.WeatherContract.WeatherEntry;
import static app.sunshine.android.example.com.sunshine.data.WeatherContract.WeatherEntry.COLUMN_DATETEXT;
import static app.sunshine.android.example.com.sunshine.data.WeatherContract.WeatherEntry.COLUMN_DEGREES;
import static app.sunshine.android.example.com.sunshine.data.WeatherContract.WeatherEntry.COLUMN_HUMIDITY;
import static app.sunshine.android.example.com.sunshine.data.WeatherContract.WeatherEntry.COLUMN_LOC_KEY;
import static app.sunshine.android.example.com.sunshine.data.WeatherContract.WeatherEntry.COLUMN_MAX_TEMP;
import static app.sunshine.android.example.com.sunshine.data.WeatherContract.WeatherEntry.COLUMN_MIN_TEMP;
import static app.sunshine.android.example.com.sunshine.data.WeatherContract.WeatherEntry.COLUMN_PRESSURE;
import static app.sunshine.android.example.com.sunshine.data.WeatherContract.WeatherEntry.COLUMN_SHORT_DESC;
import static app.sunshine.android.example.com.sunshine.data.WeatherContract.WeatherEntry.COLUMN_WEATHER_ID;
import static app.sunshine.android.example.com.sunshine.data.WeatherContract.WeatherEntry.COLUMN_WIND_SPEED;
import static app.sunshine.android.example.com.sunshine.data.WeatherContract.getDbDateString;

/**
 * Created by Araceli on 16/12/2014.
 */
public class SunshineSyncAdapter extends AbstractThreadedSyncAdapter {
    public final String LOG_TAG = SunshineSyncAdapter.class.getSimpleName();

    // Interval at which to sync with the weather, in milliseconds.
    // 60 seconds (1 minute) * 180 = 3 hours
    public static final int SYNC_INTERVAL = 60 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;

    /* Because the notification data will be pulled from the database,
    I need to add projection and column indices values here in SyncAdapter */
    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[] {
            WeatherEntry.COLUMN_WEATHER_ID,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
            WeatherEntry.COLUMN_SHORT_DESC
    };

    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;
    private static final int INDEX_SHORT_DESC = 3;

    // amount of milliseconds in a day
    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    // id you create that is matched to the notification so that I can reuse it to post at most one notification.
    private static final int WEATHER_NOTIFICATION_ID = 3004;

    public SunshineSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
    Log.d(LOG_TAG, "onPerformSync");

        String locationQuery = Utility.getPreferredLocation(getContext()); //intent.getStringExtra(LOCATION_QUERY_EXTRA);
        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;

        //Configure outside
        String format = "json";
        String units = "metric";
        int days = 14;

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast

            final String BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String Q_PARAM = "q";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";

            Uri.Builder uriBuilder = Uri.parse(BASE_URL).buildUpon()
                    .appendQueryParameter(Q_PARAM, locationQuery)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(days));
            Uri builtUri = uriBuilder.build();

            URL url = new URL(builtUri.toString());

            Log.v(LOG_TAG, "URI: " + builtUri.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return;
            }
            forecastJsonStr = buffer.toString();

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            forecastJsonStr = null;
        } finally{
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        try {
            getWeatherDataFromJson(forecastJsonStr, days, locationQuery);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }

        // This will happen if there was an error getting or parsing the forecast.
        return;

    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Parsing: constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void getWeatherDataFromJson(String forecastJsonStr, int numDays,
                                        String locationSetting)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        // Location information
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";
        // Location coordinate
        final String OWM_COORD_LAT = "lat";
        final String OWM_COORD_LONG = "lon";

        // Weather information.  Each day's forecast info is an element of the "list" array.
        final String OWM_LIST = "list";

        final String OWM_DATETIME = "dt";
        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";

        // All temperatures are children of the "temp" object.
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";

        final String OWM_WEATHER = "weather";
        final String OWM_DESCRIPTION = "main";
        final String OWM_WEATHER_ID = "id";

        // handle exceptions
        //try {
        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        // Get city name, longitude and latitude returned by openWeather
        JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
        String cityName = cityJson.getString(OWM_CITY_NAME);
        JSONObject coordJson = cityJson.getJSONObject(OWM_COORD);
        double cityLatitude = coordJson.getDouble(OWM_COORD_LAT);
        double cityLongitude = coordJson.getDouble(OWM_COORD_LONG);

        Log.v(LOG_TAG, cityName + " coord: " + cityLatitude + " " + cityLongitude);

        // Insert the location into database
        long locationID = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);

        Vector<ContentValues> vector = new Vector<ContentValues>(weatherArray.length());

        for(int i = 0; i < weatherArray.length(); i++) {
            // Values that will be collected

            long dateTime;
            double pressure;
            int humidity;
            double windSpeed;
            double windDirection;

            double high;
            double low;

            String description;
            int weatherId;

            // Get the JSON object representing the day
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            // The date/time is returned as a long.  We need to convert that
            // into something human-readable, since most people won't read "1400356800" as
            // "this saturday".
            dateTime = dayForecast.getLong(OWM_DATETIME);

            pressure = dayForecast.getDouble(OWM_PRESSURE);
            humidity = dayForecast.getInt(OWM_HUMIDITY);
            windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
            windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

            // Description is in a child array called "weather", which is 1 element long.
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);
            weatherId = weatherObject.getInt(OWM_WEATHER_ID);

            // Temperatures are in a child object called "temp".  Try not to name variables
            // "temp" when working with temperature.  It confuses everybody.
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            high = temperatureObject.getDouble(OWM_MAX);
            low = temperatureObject.getDouble(OWM_MIN);

            ContentValues weatherValues = new ContentValues();

            weatherValues.put(COLUMN_LOC_KEY, locationID);
            weatherValues.put(COLUMN_DATETEXT,
                    getDbDateString(new Date(dateTime * 1000L)));
            weatherValues.put(COLUMN_HUMIDITY, humidity);
            weatherValues.put(COLUMN_PRESSURE, pressure);
            weatherValues.put(COLUMN_WIND_SPEED, windSpeed);
            weatherValues.put(COLUMN_DEGREES, windDirection);
            weatherValues.put(COLUMN_MAX_TEMP, high);
            weatherValues.put(COLUMN_MIN_TEMP, low);
            weatherValues.put(COLUMN_SHORT_DESC, description);
            weatherValues.put(COLUMN_WEATHER_ID, weatherId);

            vector.add(weatherValues);

            if (vector.size() > 0) {
                ContentValues[] cvArray = new ContentValues[vector.size()];
                vector.toArray(cvArray);
                int rowsInserted = getContext().getContentResolver().bulkInsert(WeatherEntry.CONTENT_URI, cvArray);

                Calendar calendar = Calendar.getInstance();
                // Yesterday
                calendar.add(Calendar.DATE, -1);
                // converts an instance of a Date to a string for our database queries.
                String yesterdayDate = WeatherContract.getDbDateString(calendar.getTime());
                getContext().getContentResolver().delete(WeatherEntry.CONTENT_URI,
                        WeatherEntry.COLUMN_DATETEXT + " <= ?",
                        new String[] {yesterdayDate});

                notifyWeather();
            }
            Log.d(LOG_TAG, "Sunshine Service Completed." + vector.size() + " inserted");
        }

    }

    /**
     *  Helper method to handle insertion of a new location in the weather database.
     *
     * @param locationSetting The location string used to request updates from the server
     * @param cityName A human-readable city name
     * @param lat the latitude of the city
     * @param lon the longitude of the city
     * @return the row ID of the added location
     */
    private long addLocation(String locationSetting, String cityName, double lat, double lon) {

        // check if the location with the the city name exist
        Cursor cursor = getContext().getContentResolver().query(LocationEntry.CONTENT_URI,
                new String[]{LocationEntry._ID},
                LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[]{locationSetting},
                null);

        if(cursor.moveToFirst()) {
            Log.v(LOG_TAG, "Location exist in db");
            int locationIndex = cursor.getColumnIndex(LocationEntry._ID);
            return cursor.getLong(locationIndex);
            // if it's not, the query will return an empty set and we should insert the new
            // city name, location setting, latitude and longitude
        } else{
            ContentValues contentValues = new ContentValues();
            contentValues.put(LocationEntry.COLUMN_CITY_NAME, cityName);
            contentValues.put(LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            contentValues.put(LocationEntry.COLUMN_LATITUDE, lat);
            contentValues.put(LocationEntry.COLUMN_LONGITUDE, lon);

            // Insert into database
            Uri locInsertUri = getContext().getContentResolver().insert(LocationEntry.CONTENT_URI, contentValues);

            return ContentUris.parseId(locInsertUri);
        }
    }


    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */
            onAccountCreated(newAccount, context);

        }

        return newAccount;
    }


    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }


    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        SunshineSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }

    private void notifyWeather() {
        Context context = getContext();
        //checking the last update and notify if it' the first of the day
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);
        boolean displayNotifications  = prefs.getBoolean(displayNotificationsKey,
                Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));

        // If notification settings is turn on
        if(displayNotifications) {
            String lastNotificationKey = context.getString(R.string.pref_last_notification);
            long lastSync = prefs.getLong(lastNotificationKey, 0);

            if (System.currentTimeMillis() - lastSync >= DAY_IN_MILLIS) {
                // Last sync was more than 1 day ago, let's send a notification with the weather.
                String locationQuery = Utility.getPreferredLocation(context);

                Uri weatherUri = WeatherEntry.buildWeatherLocationWithDate(locationQuery, WeatherContract.getDbDateString(new Date()));

                // we'll query our contentProvider, for the current day
                Cursor cursor = context.getContentResolver().query(weatherUri, NOTIFY_WEATHER_PROJECTION, null, null, null);

                if (cursor.moveToFirst()) {
                    int weatherId = cursor.getInt(INDEX_WEATHER_ID);
                    double high = cursor.getDouble(INDEX_MAX_TEMP);
                    double low = cursor.getDouble(INDEX_MIN_TEMP);
                    String desc = cursor.getString(INDEX_SHORT_DESC);

                    int iconId = Utility.getIconResourceForWeatherCondition(weatherId);
                    String title = context.getString(R.string.app_name);

                    // Define the text of the forecast.
                    String contentText = String.format(context.getString(R.string.format_notification),
                            desc,
                            Utility.formatTemperature(context, high, Utility.isMetric(context)),
                            Utility.formatTemperature(context, low, Utility.isMetric(context)));

                    // NotificationCompatBuilder to build the notification.
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(context)
                                    .setSmallIcon(iconId)
                                    .setContentTitle(title)
                                    .setContentText(contentText);

                    // When user clicks on the notification, open the app
                    Intent resultIntent = new Intent(context, MainActivity.class);
                    // The stack builder object will contain an artificial back stack for the started Activity.
                    // This ensures that navigating backward from the Activity leads out of
                    // your application to the Home screen.
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                    // Adds the back stack for the Intent (but not the Intent itself)
                    stackBuilder.addParentStack(MainActivity.class);
                    // Adds the Intent that starts the Activity to the top of the stack
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent =
                            stackBuilder.getPendingIntent(
                                    0,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            );
                    mBuilder.setContentIntent(resultPendingIntent);

                    NotificationManager mNotificationManager =
                            (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
                    // ID allows you to update the notification later on.
                    mNotificationManager.notify(WEATHER_NOTIFICATION_ID, mBuilder.build());


                    //refreshing last sync
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putLong(lastNotificationKey, System.currentTimeMillis());
                    editor.commit();
                }
            }
        }
    }
}
