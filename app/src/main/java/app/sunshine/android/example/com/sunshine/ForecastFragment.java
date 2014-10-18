package app.sunshine.android.example.com.sunshine;

/**
 * Created by Araceli on 23/07/2014.
 */

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.Date;

import app.sunshine.android.example.com.sunshine.data.WeatherContract;

import static app.sunshine.android.example.com.sunshine.data.WeatherContract.LocationEntry;
import static app.sunshine.android.example.com.sunshine.data.WeatherContract.WeatherEntry;

/**
 * A Forecast fragment containing a simple view.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    // Each loader has an id
    private static final int FORECAST_LOADER = 0;
    private String mLocation;
    private ListView listView;
    private int mPosition = ListView.INVALID_POSITION;

    private static final String SELECTED_POS = "selected_position";

    // For the forecast view we're showing only a small subset of the stored data.
    // Specify the columns we need.
    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
            WeatherEntry.COLUMN_DATETEXT,
            WeatherEntry.COLUMN_SHORT_DESC,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
            LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherEntry.COLUMN_WEATHER_ID
    };

    // These indices are tied to FORECAST_COLUMNS. If FORECAST_COLUMNS changes, these
    // must change.
    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_DESC = 2;
    public static final int COL_WEATHER_MAX_TEMP = 3;
    public static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_LOCATION_SETTING = 5;
    public static final int COL_WEATHER_CONDITION_ID = 6;

    private ForecastAdapter mForecastAdapter;


    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(String date);
    }

    public ForecastFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Loaders are initialized here because their life cycle is bound to the activity, not to the fragment.
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Bind Adapter to ListView
        listView = (ListView) rootView.findViewById(R.id.listView_forecast);

        mForecastAdapter = new ForecastAdapter(getActivity(), null, 0);
        listView.setAdapter(mForecastAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Cursor cursor = mForecastAdapter.getCursor();
                // Move to the position clicked
                if(cursor != null && cursor.moveToPosition(position)) {
                    String date = cursor.getString(COL_WEATHER_DATE);
                    ((Callback)getActivity()).onItemSelected(date);
                }
                // When clicked, update the position
                mPosition = position;
            }
        });

        // If there's instance state, restore the position
        // Read position from Bundle
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_POS)) {
            // the listview probably hasn't been populated yet. Actually perform the swapout in onLoadFinished.
            mPosition = savedInstanceState.getInt(SELECTED_POS);
        }
        return rootView;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // this fragment handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateWeather() {
        FetchWeatherTask weatherTask = new FetchWeatherTask(getActivity());
        // Refactor preferences with utility class
        String location = Utility.getPreferredLocation(getActivity());

        // Pass the location into the fetch weather task
        weatherTask.execute(location);
    }

    @Override
    public void onStart() {
        super.onStart();
        // I no longer want to fetch the weather data every time the activity starts, since I'm storing it in a database now
        //updateWeather();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check to see if location preference changed when the activity resumed, and if so, restart the loader
        // that way URI is changed
        if(mLocation != null && !mLocation.equals(Utility.getPreferredLocation(getActivity()))) {
            getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.

        // Return and show only weather for dates after or including today.
        String startDate = WeatherContract.getDbDateString(new Date());

        // Sort order
        String sortOrder = WeatherEntry.COLUMN_DATETEXT + " ASC";

        String mLocation = Utility.getPreferredLocation(getActivity());

        // Build the URI with location and start date
        Uri weatherForLocationUri = WeatherEntry.buildWeatherLocationWithStartDate(mLocation, startDate);

        // Create a CursorLoader that will create a Cursor for the data being displayed
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
        mForecastAdapter.swapCursor(data);
        if (mPosition != ListView.INVALID_POSITION) {
            // Use position to scroll to selected item
            // If we don't need to restart the loader, and there's a desired position to restore to
            listView.setSelection(mPosition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mForecastAdapter.swapCursor(null);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Store the position
        // When tablets rotate, the currently selected list item needs to be saved.
        // When No item is selected, mPosition will be set to Listview.INVALID_POSITION,
        if (mPosition != ListView.INVALID_POSITION) {
            savedInstanceState.putInt(SELECTED_POS, mPosition);
        }

        super.onSaveInstanceState(savedInstanceState);
    }
}