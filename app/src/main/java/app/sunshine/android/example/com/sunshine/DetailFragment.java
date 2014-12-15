package app.sunshine.android.example.com.sunshine;

/**
 * Created by Araceli on 13/10/2014.
 */

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import app.sunshine.android.example.com.sunshine.data.WeatherContract;


public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String LOG_TAG = DetailFragment.class.getSimpleName();

    public static final String DATE = "forecast_date";
    public static final String LOCATION = "location";

    public static final String SHARE_HASHTAG = " #SunshineApp";
    private String mDayForecast;
    private String mLocation;
    private static final int DETAIL_LOADER = 0;
    private ShareActionProvider mShareActionProvider;


    private static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATETEXT,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING
    };

    private TextView mDate;
    private TextView mMax;
    private TextView mMin;
    private ImageView mIcon;
    private TextView mDesc;
    private TextView mCustomDate;
    private TextView mHumidity;
    private TextView mPressure;
    private TextView mWind;


    public DetailFragment() {
        setHasOptionsMenu(true);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        // When rotate device location is lost. Preserve it here in the bundle.
        outState.putString(LOCATION, mLocation);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mLocation = savedInstanceState.getString("location");
        }
        // Remove reliance with incoming intent. Use arguments bundle instead.
        Bundle bundle = getArguments();
        // If bundle is null we don't init loader
        if (bundle != null && bundle.containsKey(DetailActivity.DATE)) {
            getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        mDate = (TextView) rootView.findViewById(R.id.detail_date_textview);
        mCustomDate = (TextView) rootView.findViewById(R.id.detail_day);
        mMax = (TextView) rootView.findViewById(R.id.detail_max_textview);
        mMin = (TextView) rootView.findViewById(R.id.detail_min_textview);
        mIcon = (ImageView) rootView.findViewById(R.id.detail_icon);
        mDesc = (TextView) rootView.findViewById(R.id.detail_desc_textview);

        mHumidity = (TextView) rootView.findViewById(R.id.detail_humidity_textview);
        mPressure = (TextView) rootView.findViewById(R.id.detail_pressure_textview);
        mWind = (TextView) rootView.findViewById(R.id.detail_wind_textview);

        /* Intent intent = getActivity().getIntent();
           if(intent != null && intent.hasExtra(DetailActivity.DATE)) {
                mDayForecast = intent.getStringExtra(DetailActivity.DATE);
                ((TextView)rootView.findViewById(R.id.weekDay)).setText(mDayForecast);
            }*/
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check to see if location preference changed when the activity resumed, and if so, restart the loader
        // that way URI is changed
        // Not restart loader if the intent is null (the Detail fragment can now exist in main activity)
        Bundle bundle = getArguments();
        if(bundle != null && bundle.containsKey(DetailActivity.DATE) &&
                mLocation != null && !mLocation.equals(Utility.getPreferredLocation(getActivity()))) {
            getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.detailfragment, menu);

        // Retrieve the share menu item
        MenuItem shareItem = menu.findItem(R.id.action_share);
        // Get the provider and hold onto it to set/change the share intent.
        ShareActionProvider mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);

        // Attach an intent to the  ShareActionProvider
        // If onLoadFinished happens before this, we can go ahead and set the share intent now.
        if(mDayForecast != null) {
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        } else {
            Log.d(LOG_TAG, "Share Action Provider is null");
        }
    }

    private Intent createShareForecastIntent() {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mDayForecast + SHARE_HASHTAG);

        return shareIntent;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
/*      Intent intent = getActivity().getIntent();
        if (intent == null || !intent.hasExtra(DATE)) {
            return null;
        }
        String forecastDate = intent.getStringExtra(DATE);*/

        String forecastDate = getArguments().getString(DetailActivity.DATE);

        mLocation = Utility.getPreferredLocation(getActivity());

        // Build the URI with location and start date
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(mLocation, forecastDate);
        Log.v(LOG_TAG, weatherForLocationUri.toString());

        // Sort order
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATETEXT + " ASC";

        return new CursorLoader(getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder
        );

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            Log.v(LOG_TAG, "no data");
            return;
        }
        // Read weather condition ID
        int weatherId = data.getInt(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID));
        // get the ion that fits with the id
        mIcon.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId));

        String dateStr = data.getString(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATETEXT));
        mDate.setText(Utility.getFormattedMonthDay(getActivity(), dateStr));
        mCustomDate.setText(Utility.getDayName(getActivity(), dateStr));

        String description = data.getString(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC));
        mDesc.setText(description);

        // For accessibility, content description
        mIcon.setContentDescription(description);

        boolean isMetric = Utility.isMetric(getActivity());
        String max = Utility.formatTemperature(getActivity(), data.getDouble(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP)), isMetric);
        mMax.setText(max);

        String min = Utility.formatTemperature(getActivity(), data.getDouble(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP)), isMetric);
        mMin.setText(min);

        float humidity = data.getFloat(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_HUMIDITY));
        mHumidity.setText(getActivity().getString(R.string.format_humidity, humidity));

        float pressure = data.getFloat(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_PRESSURE));
        mPressure.setText(getActivity().getString(R.string.format_pressure, pressure));

        float windSpeed = data.getFloat(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED));
        float windDegrees = data.getFloat(data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DEGREES));
        mWind.setText(Utility.getFormattedWind(getActivity(), windSpeed, windDegrees));

        // Format for share intent
        mDayForecast = String.format("%s - %s - %s/%s", dateStr, description, max, min);

        Log.v(LOG_TAG, "Forecast String: " + mDayForecast);

        // If onCreateOptionsMenu has already happened, we need to update the share intent now.
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}