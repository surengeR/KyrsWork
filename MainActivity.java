package com.edouard.weatherapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Random;

import static com.edouard.weatherapp.SearchCity.CITY_NAME;
import static com.edouard.weatherapp.SearchCity.ORIGIN;

public class    MainActivity extends AppCompatActivity
{

    public static final String API_KEY = "&APPID=3e668d70651c617968484ba7909558a3&units=metric";
    private static final String CNT = "&cnt=5";
    private static final String CURRENT_LOCATION_API_URL = "http://api.openweathermap.org/data/2.5/weather?lat=";
    private static final String CITY_API_URL = "http://api.openweathermap.org/data/2.5/weather?q=";
    private static final String CITY_FORECAST_API_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?q=";
    private static final String CURRENT_LOCATION_FORECAST_API_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?lat=";
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1000;
    private static final int PERMISSION_REQUEST_CODE = 1;
    public static final int SWIPE_MIN_DISTANCE = 200;
    public static final int SWIPE_THRESHOLD_VELOCITY = 200;
    public static final String DEBUG_TAG = "DEBUG";
    public static final String DISPLAYED_LOCATION = "DISPLAYED_LOCATION";
    private static final String IS_TUTORIAL_DONE = "IS_TUTORIAL_DONE";
    private String currentDisplayedLocation = null;
    private GestureDetectorCompat mDetector;
    private String[] sunWarm;
    private String[] sunCold;
    private String[] rainWarm;
    private String[] rainCold;
    private String[] snowWarm;
    private String[] snowCold;
    private String[] mist;
    private boolean fromSync = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        RelativeLayout tutorialView = (RelativeLayout) findViewById(R.id.tutorialView);
        ImageView myLocationButton = (ImageView) findViewById(R.id.getCurrentLocationInfo);
        ImageView searchButton = (ImageView) findViewById(R.id.searchButton);
        Button syncButton = (Button) findViewById(R.id.syncButton);
        TextView aboutText = (TextView) findViewById(R.id.aboutView);
        tutorialView.bringToFront();
        myLocationButton.bringToFront();
        syncButton.setVisibility(View.VISIBLE);
        searchButton.bringToFront();

        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/Time Burner Normal.ttf");
        TextView tutorialAppName = (TextView) findViewById(R.id.tutorialAppName);
        TextView tutorialMyLocationText = (TextView) findViewById(R.id.tutorialMyLocationText);
        TextView tutorialSwipeTextView = (TextView) findViewById(R.id.tutorialSwipeTextView);
        TextView tutorialSearchText = (TextView) findViewById(R.id.tutorialSearchText);
        TextView tutorialSyncText = (TextView) findViewById(R.id.tutorialSyncText);
        TextView tutorialAboutText = (TextView) findViewById(R.id.tutorialAboutText);
        aboutText.setTypeface(font);
        tutorialAppName.setTypeface(font);
        tutorialMyLocationText.setTypeface(font);
        tutorialSwipeTextView.setTypeface(font);
        tutorialSearchText.setTypeface(font);
        tutorialSyncText.setTypeface(font);
        tutorialAboutText.setTypeface(font);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        Boolean tutorialShown = preferences.getBoolean(IS_TUTORIAL_DONE, false);
        if (!tutorialShown) {
            tutorialView.setVisibility(View.VISIBLE);
        } else {
            tutorialView.setVisibility(View.GONE);
        }

        mDetector = new GestureDetectorCompat(this, new MyGestureListener());

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
        }
        else
        {
            Location location = getLocation();
            if(location != null)
            {
                String locationString = location.getLatitude() + "&lon=" + location.getLongitude();
                new GetCurrentFeed().execute(CURRENT_LOCATION_API_URL, locationString);
                new GetForecastFeed().execute(CURRENT_LOCATION_FORECAST_API_URL, locationString);
            }
            else
            {
                displayDefaultLocationInfo();
            }
        }

        Intent intent = getIntent();
        String origin = intent.getStringExtra(ORIGIN);
        if(origin != null)
        {
            String cityName = intent.getStringExtra(CITY_NAME);
            new GetCurrentFeed().execute(CITY_API_URL, cityName);
            new GetForecastFeed().execute(CITY_FORECAST_API_URL, cityName);
        }

        sunWarm = getResources().getStringArray(R.array.sunWarm);
        sunCold = getResources().getStringArray(R.array.sunCold);
        rainWarm = getResources().getStringArray(R.array.rainWarm);
        rainCold = getResources().getStringArray(R.array.rainCold);
        snowWarm = getResources().getStringArray(R.array.snowWarm);
        snowCold = getResources().getStringArray(R.array.snowCold);
        mist = getResources().getStringArray(R.array.mist);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
    {
        switch(requestCode)
        {
            case PERMISSION_REQUEST_CODE:
            {
                // If request is cancelled, the result arrays are empty.
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Location location = getLocation();
                    if(location != null)
                    {
                        String locationString = location.getLatitude() + "&lon=" + location.getLongitude();
                        new GetCurrentFeed().execute(CURRENT_LOCATION_API_URL, locationString);
                        new GetForecastFeed().execute(CURRENT_LOCATION_FORECAST_API_URL, locationString);
                    }
                    else
                    {
                        displayDefaultLocationInfo();
                    }
                }
                else
                {
                    displayDefaultLocationInfo();
                }
                break;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public Location getLocation()
    {
        Location location = null;
        LocationListener listener = new LocationListener()
        {
            @Override
            public void onLocationChanged(Location location)
            {

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle)
            {

            }

            @Override
            public void onProviderEnabled(String s)
            {

            }

            @Override
            public void onProviderDisabled(String s)
            {

            }
        };
        try
        {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
            }

            // getting GPS status
            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            Log.e("GPS_ENABLED", String.valueOf(isGPSEnabled));
            Log.e("NETWORK_ENABLED", String.valueOf(isNetworkEnabled));

            if(!isGPSEnabled && !isNetworkEnabled)
            {
                displayDefaultLocationInfo();
            }
            else
            {
                // if GPS Enabled get lat/long using GPS Services
                if(isGPSEnabled)
                {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, listener);
                    Log.d("GPS", "GPS Enabled");
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
                if(isNetworkEnabled)
                {
                    if(location == null)
                    {
                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, listener);
                        Log.d("Network", "Network Enabled");
                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                }
            }

        } catch(Exception e)
        {
            e.printStackTrace();
        }

        return location;
    }

    public void getCurrentLocationInfo(View view)
    {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
        }
        else
        {
            Location location = getLocation();
            if(location != null)
            {
                String locationString = location.getLatitude() + "&lon=" + location.getLongitude();
                new GetCurrentFeed().execute(CURRENT_LOCATION_API_URL, locationString);
                new GetForecastFeed().execute(CURRENT_LOCATION_FORECAST_API_URL, locationString);
            }
            else
            {
                displayDefaultLocationInfo();
            }
        }
    }

    public void onTutorialOkClick(View view)
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        preferences.edit().putBoolean(IS_TUTORIAL_DONE, true).apply();
        RelativeLayout tutorialView = (RelativeLayout) findViewById(R.id.tutorialView);
        tutorialView.setVisibility(View.GONE);
    }

    private class GetCurrentFeed extends AsyncTask<String, Void, String>
    {

        @Override
        protected void onPreExecute()
        {
            if(!fromSync)
            {
                Toast.makeText(MainActivity.this, "Getting location data", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected String doInBackground(String... strings)
        {
            String request = strings[0];
            String locationString = strings[1];

            try
            {
                URL url = new URL(request + locationString + API_KEY);
                Log.d("URL", url.toString());
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try
                {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while((line = bufferedReader.readLine()) != null)
                    {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();
                } finally
                {
                    urlConnection.disconnect();
                }
            } catch(Exception e)
            {
                Log.e("ERROR", e.getMessage(), e);
                return null;
            }
        }

        protected void onPostExecute(String response)
        {
            if(response == null)
            {
                response = "Could not retrieve data";
            }
            Log.i("INFO", response);
            try
            {
                parseIndividualWeatherJSON(response);
            } catch(JSONException e)
            {
                e.printStackTrace();
                Log.e("JSON_ERROR", e.toString());
                displayErrorToast();
            }
        }
    }

    private class GetForecastFeed extends AsyncTask<String, Void, String>
    {

        @Override
        protected void onPreExecute()
        {

        }

        @Override
        protected String doInBackground(String... strings)
        {
            String request = strings[0];
            String locationString = strings[1];

            try
            {
                URL url = new URL(request + locationString + API_KEY + CNT);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try
                {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while((line = bufferedReader.readLine()) != null)
                    {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();
                } finally
                {
                    urlConnection.disconnect();
                }
            } catch(Exception e)
            {
                Log.e("ERROR", e.getMessage(), e);
                return null;
            }
        }

        protected void onPostExecute(String response)
        {
            if(response == null)
            {
                response = "Could not retrieve data";
            }
            Log.i("INFO", response);
            try
            {
                parseForecastWeatherJSON(response);
            } catch(JSONException e)
            {
                e.printStackTrace();
                Log.e("JSON_ERROR", e.toString());
                displayErrorToast();
            }
        }
    }

    private void displayErrorToast()
    {
        Toast.makeText(this, R.string.cityNotFound, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, SearchCity.class);
        startActivity(intent);
    }

    public void parseIndividualWeatherJSON(String weatherJSON) throws JSONException
    {
        JSONObject object = new JSONObject(weatherJSON);
        if(!object.getString("cod").equals("200"))
        {
            Toast.makeText(this, R.string.cityNotFound, Toast.LENGTH_SHORT).show();
            Location location = getLocation();
            if(location != null)
            {
                String locationString = location.getLatitude() + "&lon=" + location.getLongitude();
                new GetCurrentFeed().execute(CURRENT_LOCATION_API_URL, locationString);
                new GetForecastFeed().execute(CURRENT_LOCATION_FORECAST_API_URL, locationString);
            }
            else
            {
                displayDefaultLocationInfo();
            }
        }

        JSONObject main = object.getJSONObject("main");
        JSONObject sys = object.getJSONObject("sys");
        String cityName = object.getString("name");
        String countryName = sys.getString("country");
        double pressureValueDouble = main.getDouble("pressure");
        int pressureValue = (int) pressureValueDouble;
        double humidityValueDouble = main.getDouble("humidity");
        int humidityValue = (int) humidityValueDouble;
        double temperatureValueDouble = main.getDouble("temp");
        int temperatureValue = (int) temperatureValueDouble;
        long dt = object.getLong("dt");
        String dataDate = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US).format(new java.util.Date(dt * 1000));

        String dataDateText = getResources().getString(R.string.data_date) + " " + dataDate;
        String location = cityName + ", " + countryName;
        currentDisplayedLocation = cityName + "," + countryName;
        String icon = getIcon(object.getJSONArray("weather").getJSONObject(0).getString("icon"));
        String funnyText = getText(object.getJSONArray("weather").getJSONObject(0).getString("icon"), temperatureValue);
        String temperature = "🌡" + String.valueOf(temperatureValue) + "°C";
        String pressure = "\uD83D\uDCA8" + pressureValue + " hPa";
        //String pressure = "\uD83D\uDCA8" + pressureValue + " hPa";
        String humidity = "\uD83D\uDCA7" + humidityValue + "%";

        TextView locationView = (TextView) findViewById(R.id.locationView);
        locationView.setText(location);
        TextView iconView = (TextView) findViewById(R.id.iconView);
        iconView.setText(icon);
        TextView temperatureView = (TextView) findViewById(R.id.temperatureView);
        temperatureView.setText(temperature);
        TextView pressureView = (TextView) findViewById(R.id.pressureView);
        pressureView.setText(pressure);
        TextView humidityView = (TextView) findViewById(R.id.humidityView);
        humidityView.setText(humidity);
        TextView funnyTextView = (TextView) findViewById(R.id.funnyTextView);
        funnyTextView.setText(funnyText);
        TextView dataDateView = (TextView) findViewById(R.id.dataDateView);
        dataDateView.setText(dataDateText);

        Typeface font4 = Typeface.createFromAsset(getAssets(), "fonts/Time Burner Normal.ttf");
        Typeface details = Typeface.createFromAsset(getAssets(), "fonts/Time Burner Bold.ttf");

        locationView.setTypeface(details);
        iconView.setTypeface(details);
        temperatureView.setTypeface(details);
        pressureView.setTypeface(details);
        humidityView.setTypeface(details);
        funnyTextView.setTypeface(font4);
        dataDateView.setTypeface(details);
    }

    public void parseForecastWeatherJSON(String weatherJSON) throws JSONException
    {
        Typeface font1 = Typeface.createFromAsset(getAssets(), "fonts/Time Burner Normal.ttf");
        TextView tempMax1 = (TextView) findViewById(R.id.tempMax1);
        TextView tempMax2 = (TextView) findViewById(R.id.tempMax2);
        TextView tempMax3 = (TextView) findViewById(R.id.tempMax3);
        TextView tempMax4 = (TextView) findViewById(R.id.tempMax4);
        TextView tempMax5 = (TextView) findViewById(R.id.tempMax5);
        TextView[] tempMax = {tempMax1, tempMax2, tempMax3, tempMax4, tempMax5};
        TextView tempMin1 = (TextView) findViewById(R.id.tempMin1);
        TextView tempMin2 = (TextView) findViewById(R.id.tempMin2);
        TextView tempMin3 = (TextView) findViewById(R.id.tempMin3);
        TextView tempMin4 = (TextView) findViewById(R.id.tempMin4);
        TextView tempMin5 = (TextView) findViewById(R.id.tempMin5);
        TextView[] tempMin = {tempMin1, tempMin2, tempMin3, tempMin4, tempMin5};
        TextView icon1 = (TextView) findViewById(R.id.icon1);
        TextView icon2 = (TextView) findViewById(R.id.icon2);
        TextView icon3 = (TextView) findViewById(R.id.icon3);
        TextView icon4 = (TextView) findViewById(R.id.icon4);
        TextView icon5 = (TextView) findViewById(R.id.icon5);
        TextView[] icon = {icon1, icon2, icon3, icon4, icon5};
        TextView day1 = (TextView) findViewById(R.id.day1);
        TextView day2 = (TextView) findViewById(R.id.day2);
        TextView day3 = (TextView) findViewById(R.id.day3);
        TextView day4 = (TextView) findViewById(R.id.day4);
        TextView day5 = (TextView) findViewById(R.id.day5);
        TextView[] day = {day1, day2, day3, day4, day5};

        JSONObject object = new JSONObject(weatherJSON);
        JSONArray list = object.getJSONArray("list");
        for(int i = 0; i < list.length(); i++)
        {
            JSONObject current = list.getJSONObject(i);
            JSONObject temperature = current.getJSONObject("temp");
            double maxDouble = temperature.getDouble("max");
            int max = (int) maxDouble;
            String maxT = max + "°C";
            double minDouble = temperature.getDouble("min");
            int min = (int) minDouble;
            String minT = min + "°C";
            JSONArray weather = current.getJSONArray("weather");
            String iconCurrent = getIcon(weather.getJSONObject(0).getString("icon"));
            long dt = current.getLong("dt");
            String date = new java.text.SimpleDateFormat("EEE", Locale.US).format(new java.util.Date(dt * 1000));
            tempMax[i].setText(maxT);
            tempMin[i].setText(minT);
            icon[i].setText(iconCurrent);
            day[i].setText(date);
            tempMax[i].setTypeface(font1);
            tempMin[i].setTypeface(font1);
            icon[i].setTypeface(font1);
            day[i].setTypeface(font1);
        }
        day[0].setText(R.string.today);
    }

    private String getText(String string, double temp)
    {
        Random generator = new Random();
        int i = generator.nextInt(3);
        switch(string)
        {
            case "01d":
                if(temp > 15)
                {
                    string = sunWarm[i];
                }
                else
                {
                    string = sunCold[i];
                }
                break;
            case "02d":
                if(temp > 15)
                {
                    string = sunWarm[i];
                }
                else
                {
                    string = sunCold[i];
                }
                break;
            case "03d":
                if(temp > 15)
                {
                    string = sunWarm[i];
                }
                else
                {
                    string = sunCold[i];
                }
                break;
            case "04d":
                if(temp > 15)
                {
                    string = sunWarm[i];
                }
                else
                {
                    string = sunCold[i];
                }
                break;
            case "09d":
                if(temp > 15)
                {
                    string = rainWarm[i];
                }
                else
                {
                    string = rainCold[i];
                }
                break;
            case "10d":
                if(temp > 15)
                {
                    string = rainWarm[i];
                }
                else
                {
                    string = rainCold[i];
                }
                break;
            case "11d":
                if(temp > 15)
                {
                    string = rainWarm[i];
                }
                else
                {
                    string = rainCold[i];
                }
                break;
            case "13d":
                if(temp > -5)
                {
                    string = snowWarm[i];
                }
                else
                {
                    string = snowCold[i];
                }
                break;
            case "50d":
                string = mist[i];
                break;
            case "01n":
                if(temp > 15)
                {
                    string = sunWarm[i];
                }
                else
                {
                    string = sunCold[i];
                }
                break;
            case "02n":
                if(temp > 15)
                {
                    string = sunWarm[i];
                }
                else
                {
                    string = sunCold[i];
                }
                break;
            case "03n":
                if(temp > 15)
                {
                    string = sunWarm[i];
                }
                else
                {
                    string = sunCold[i];
                }
                break;
            case "04n":
                if(temp > 15)
                {
                    string = sunWarm[i];
                }
                else
                {
                    string = sunCold[i];
                }
                break;
            case "09n":
                if(temp > 15)
                {
                    string = rainWarm[i];
                }
                else
                {
                    string = rainCold[i];
                }
                break;
            case "10n":
                if(temp > 15)
                {
                    string = rainWarm[i];
                }
                else
                {
                    string = rainCold[i];
                }
                break;
            case "11n":
                if(temp > 15)
                {
                    string = rainWarm[i];
                }
                else
                {
                    string = rainCold[i];
                }
                break;
            case "13n":
                if(temp > -5)
                {
                    string = snowWarm[i];
                }
                else
                {
                    string = snowCold[i];
                }
                break;
            case "50n":
                string = mist[i];
                break;
        }
        return string;
    }

    private String getIcon(String string)
    {
        switch(string)
        {
            case "01d":
                string = "☀";
                break;
            case "02d":
                string = "🌤";
                break;
            case "03d":
                string = "⛅";
                break;
            case "04d":
                string = "🌥";
                break;
            case "09d":
                string = "🌦";
                break;
            case "10d":
                string = "🌧";
                break;
            case "11d":
                string = "⛈";
                break;
            case "13d":
                string = "🌨";
                break;
            case "50d":
                string = "\uD83C\uDF01";
                break;
            case "01n":
                string = "🌕";
                break;
            case "02n":
                string = "🌤";
                break;
            case "03n":
                string = "🌤";
                break;
            case "04n":
                string = "🌥";
                break;
            case "09n":
                string = "🌦";
                break;
            case "10n":
                string = "🌧";
                break;
            case "11n":
                string = "⛈";
                break;
            case "13n":
                string = "🌨";
                break;
            case "50n":
                string = "\uD83C\uDF01";
                break;
        }
        return string;
    }

    private void displayDefaultLocationInfo()
    {
        //Toast.makeText(this, R.string.permission, Toast.LENGTH_LONG).show();
        String locationString = "Odessa,UA";
        new GetCurrentFeed().execute(CITY_API_URL, locationString);
        new GetForecastFeed().execute(CITY_FORECAST_API_URL, locationString);
    }

    public void addLocation(View view)
    {
        Intent intent = new Intent(this, SearchCity.class);
        intent.putExtra(DISPLAYED_LOCATION, currentDisplayedLocation);
        startActivity(intent);
        overridePendingTransition(R.anim.to_search_enter, R.anim.to_search_exit);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener
    {

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY)
        {
            try
            {
                float diff = event1.getX() - event2.getX();

                // Left swipe
                if(diff > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)
                {
                    MainActivity.this.onLeftSwipe();
                }

            } catch(Exception e)
            {
                Log.e("Home", "Error on gestures");
            }
            return true;
        }
    }

    private void onLeftSwipe()
    {
        Intent intent = new Intent(this, SearchCity.class);
        intent.putExtra(DISPLAYED_LOCATION, currentDisplayedLocation);
        startActivity(intent);
        overridePendingTransition(R.anim.to_search_enter, R.anim.to_search_exit);
    }

    public void syncData(View view)
    {
        fromSync = true;
        Toast.makeText(this, "Getting latest data", Toast.LENGTH_LONG).show();
        new GetCurrentFeed().execute(CITY_API_URL, currentDisplayedLocation);
        new GetForecastFeed().execute(CITY_FORECAST_API_URL, currentDisplayedLocation);
        fromSync = false;
    }

    public void onAboutClick(View view)
    {
        Intent intent = new Intent(this, AboutActivity.class);
        intent.putExtra(DISPLAYED_LOCATION, currentDisplayedLocation);
        startActivity(intent);
        overridePendingTransition(R.anim.to_search_enter, R.anim.to_search_exit);
    }

    @Override
    public void onBackPressed()
    {
        moveTaskToBack(true);
        finish();
    }
}
