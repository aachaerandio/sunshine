package app.sunshine.android.example.com.sunshine;

/**
 * Created by Araceli on 25/07/2014.
 */
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WeatherDataParser {

    /**
     * Given a string of the form returned by the api call:
     * http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7
     * retrieve the maximum temperature for the day indicated by dayIndex
     * (Note: 0-indexed, so 0 would refer to the first day).
     */
    public static double getMaxTemperatureForDay(String weatherJsonStr, int dayIndex)
            throws JSONException {
        // TODO: add parsing code here
        JSONObject jsonObj = new JSONObject(weatherJsonStr);

        JSONArray days = jsonObj.getJSONArray("list");
        JSONObject weather = days.getJSONObject(dayIndex);
        JSONObject temp = weather.getJSONObject("temp");
        return temp.getDouble("max");
    }

}