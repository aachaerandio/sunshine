package app.sunshine.android.example.com.sunshine;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.w3c.dom.Text;

import app.sunshine.android.example.com.sunshine.data.WeatherContract;

import static app.sunshine.android.example.com.sunshine.data.WeatherContract.*;

public class DetailActivity extends ActionBarActivity {

    public static final String DATE = "forecast_date";
    public static final String LOCATION = "location";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DetailFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

        public static final String LOG_TAG = DetailFragment.class.getSimpleName();
        public static final String SHARE_HASHTAG = " #SunshineApp";
        private String mDayForecast;
        private String mLocation;
        private static final int DETAIL_LOADER = 0;
        private ShareActionProvider mShareActionProvider;

        private static final String[] FORECAST_COLUMNS = {
                WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
                WeatherEntry.COLUMN_DATETEXT,
                WeatherEntry.COLUMN_SHORT_DESC,
                WeatherEntry.COLUMN_MAX_TEMP,
                WeatherEntry.COLUMN_MIN_TEMP,
                WeatherEntry.COLUMN_HUMIDITY,
                WeatherEntry.COLUMN_PRESSURE,
                WeatherEntry.COLUMN_WIND_SPEED,
                WeatherEntry.COLUMN_DEGREES,
                WeatherEntry.COLUMN_WEATHER_ID
        };

        public DetailFragment() {
            setHasOptionsMenu(true);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            // When rotate device location is lost. I can preserve it here in the bundle.
            outState.putString(LOCATION, mLocation);
            super.onSaveInstanceState(outState);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            getLoaderManager().initLoader(DETAIL_LOADER, null, this);
            if (savedInstanceState != null) {
                mLocation = savedInstanceState.getString("location");
            }
            super.onActivityCreated(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
/*         Intent intent = getActivity().getIntent();
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
            if(mLocation != null && !mLocation.equals(Utility.getPreferredLocation(getActivity()))) {
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
            Log.v(LOG_TAG, "In onCreateLoader");

            Intent intent = getActivity().getIntent();
            if (intent == null || !intent.hasExtra(DATE)) {
                return null;
            }
            String forecastDate = intent.getStringExtra(DATE);
            Log.v(LOG_TAG, forecastDate);
            mLocation = Utility.getPreferredLocation(getActivity());

            // Build the URI with location and start date
            Uri weatherForLocationUri = WeatherEntry.buildWeatherLocationWithDate(mLocation, forecastDate);
            Log.v(LOG_TAG, weatherForLocationUri.toString());

            // Sort order
            String sortOrder = WeatherEntry.COLUMN_DATETEXT + " ASC";

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
            String dateStr = Utility.formatDate(data.getString(data.getColumnIndex(WeatherEntry.COLUMN_DATETEXT)));
            ((TextView)getView().findViewById(R.id.detail_date_textview)).setText(dateStr);

            String description = data.getString(data.getColumnIndex(WeatherEntry.COLUMN_SHORT_DESC));
            ((TextView)getView().findViewById(R.id.detail_desc_textview)).setText(description);

            boolean isMetric = Utility.isMetric(getActivity());
            String max = Utility.formatTemperature(data.getDouble(data.getColumnIndex(WeatherEntry.COLUMN_MAX_TEMP)), isMetric);
            ((TextView)getView().findViewById(R.id.detail_max_textview)).setText(max);

            String min = Utility.formatTemperature(data.getDouble(data.getColumnIndex(WeatherEntry.COLUMN_MIN_TEMP)), isMetric);
            ((TextView)getView().findViewById(R.id.detail_min_textview)).setText(min);

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
}
