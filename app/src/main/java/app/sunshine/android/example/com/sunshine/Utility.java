package app.sunshine.android.example.com.sunshine;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Araceli on 19/08/2014.
 */
public class Utility {

    public static String getPreferredLocation(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(context.getString(R.string.pref_location_key),
                context.getString(R.string.pref_location_default));

    }
}
