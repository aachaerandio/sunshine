package app.sunshine.android.example.com.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity implements ForecastFragment.Callback{

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    public static boolean mTwoPane;
    DetailFragment detailFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
/*        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.container, new ForecastFragment()).commit();
        }*/
        if(findViewById(R.id.weather_detail_container) != null) {
            // Detail container view will be present only in large-screen layouts.
            // If this view is present, the activity should be in two-pane mode
            mTwoPane = true;

            // show the detail view in this activity
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.weather_detail_container, new DetailFragment()).commit();
            }
        } else {
            mTwoPane = false;
        }

        ForecastFragment forecastFragment = (ForecastFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_forecast);
        forecastFragment.setUseTodayLayout(!mTwoPane);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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
        if (id == R.id.action_map) {
            viewLocationOnMap();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void viewLocationOnMap() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String location = sharedPref.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));

        Uri.Builder uriBuilder =  Uri.parse("geo:0,0?").buildUpon().appendQueryParameter("q", location);
        Uri geoLocation = uriBuilder.build();

        Intent mapIntent = new Intent(Intent.ACTION_VIEW);
        mapIntent.setData(geoLocation);
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Log.d(LOG_TAG, "Coudn't open " + location);
        }
    }


    @Override
    public void onItemSelected(String date) {

        if (mTwoPane) { // Replace DetailFragment using a fragment transaction
            // Make new fragment to show this selection.
            Bundle bundle = new Bundle();
            bundle.putString(DetailActivity.DATE, date);
            detailFragment = new DetailFragment();
            detailFragment.setArguments(bundle);

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.weather_detail_container, detailFragment);

            ft.commit();

        } else { // Launch Detail Activity
            Intent detailIntent = new Intent(this, DetailActivity.class);
            detailIntent.putExtra(DetailActivity.DATE, date);
            startActivity(detailIntent);
        }


    }
}
