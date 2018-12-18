package com.edouard.weatherapp;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import static com.edouard.weatherapp.MainActivity.API_KEY;
import static com.edouard.weatherapp.MainActivity.DISPLAYED_LOCATION;
import static com.edouard.weatherapp.MainActivity.SWIPE_MIN_DISTANCE;
import static com.edouard.weatherapp.MainActivity.SWIPE_THRESHOLD_VELOCITY;

public class SearchCity extends AppCompatActivity
{

    public static final String CITY_NAME = "CITY_NAME";
    public static final String ORIGIN = "ORIGIN";
    public static final String SEARCH = "SEARCH";
    public static final String FIND_URL = "http://api.openweathermap.org/data/2.5/find?q=";
    public static final String TYPE_LIKE = "&type=like";
    private String displayedLocation;
    private GestureDetectorCompat mDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_city);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mDetector = new GestureDetectorCompat(this, new MyGestureListener());

        final ListView listView = (ListView) findViewById(R.id.locationsListView);
        listView.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent)
            {
                return mDetector.onTouchEvent(motionEvent);
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                Object object = listView.getItemAtPosition(i);
                String locationName = object.toString();
                locationName = locationName.replaceAll("\\s+","");
                Intent intent = new Intent(SearchCity.this, MainActivity.class);
                intent.putExtra(CITY_NAME, locationName);
                intent.putExtra(ORIGIN, SEARCH);
                startActivity(intent);
                overridePendingTransition(R.anim.back_home_enter, R.anim.back_home_exit);
                finish();
            }
        });

        Intent intent = getIntent();
        displayedLocation = intent.getStringExtra(DISPLAYED_LOCATION);
    }

    public void backHome(View view)
    {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(CITY_NAME, displayedLocation);
        intent.putExtra(ORIGIN, SEARCH);
        startActivity(intent);
        overridePendingTransition(R.anim.back_home_enter, R.anim.back_home_exit);
        finish();
    }

    public void addCity(View view)
    {
        EditText cityInput = (EditText) findViewById(R.id.cityInput);
        String cityName = cityInput.getText().toString().toLowerCase();
        if(cityName.isEmpty())
        {
            cityInput.setError("Field cannot be empty!");
            cityInput.requestFocus();
        }
        else
        {
            new GetCurrentFeed().execute(FIND_URL, cityName);
            /*Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(CITY_NAME, cityName);
            intent.putExtra(ORIGIN, SEARCH);
            startActivity(intent);
            overridePendingTransition(R.anim.back_home_enter, R.anim.back_home_exit);
            finish();*/
        }
    }

    private class GetCurrentFeed extends AsyncTask<String, Void, String>
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
                URL url = new URL(request + locationString + TYPE_LIKE + API_KEY);
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

    private void displayErrorToast()
    {
        Toast.makeText(this, R.string.cityNotFound, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, SearchCity.class);
        startActivity(intent);
    }

    public void parseIndividualWeatherJSON(String weatherJSON) throws JSONException
    {
        ArrayList<String> locations = new ArrayList<>();
        JSONObject object = new JSONObject(weatherJSON);
        if(!object.getString("cod").equals("200"))
        {
            Toast.makeText(this, R.string.cityNotFound, Toast.LENGTH_SHORT).show();
            return;
        }

        JSONArray list = object.getJSONArray("list");
        for(int i = 0; i < list.length(); i++)
        {
            JSONObject currentObject = list.getJSONObject(i);
            JSONObject sys = currentObject.getJSONObject("sys");
            String cityName = currentObject.getString("name");
            String countryName = sys.getString("country");
            String location = cityName + ", " + countryName;
            locations.add(location);
        }
        ListView listView = (ListView) findViewById(R.id.locationsListView);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, locations);
        listView.setAdapter(arrayAdapter);
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener
    {

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY)
        {
            try
            {
                float diff = event1.getX() - event2.getX();

                // Right swipe
                if(-diff > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)
                {
                    SearchCity.this.onRightSwipe();
                }

            } catch(Exception e)
            {
                Log.e("Home", "Error on gestures");
            }
            return true;
        }
    }

    private void onRightSwipe()
    {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(CITY_NAME, displayedLocation);
        intent.putExtra(ORIGIN, SEARCH);
        startActivity(intent);
        overridePendingTransition(R.anim.back_home_enter, R.anim.back_home_exit);
        finish();
    }
}
