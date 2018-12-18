package com.edouard.weatherapp;

import android.content.Intent;
import android.graphics.Typeface;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import static com.edouard.weatherapp.MainActivity.DISPLAYED_LOCATION;
import static com.edouard.weatherapp.MainActivity.SWIPE_MIN_DISTANCE;
import static com.edouard.weatherapp.MainActivity.SWIPE_THRESHOLD_VELOCITY;
import static com.edouard.weatherapp.SearchCity.CITY_NAME;
import static com.edouard.weatherapp.SearchCity.ORIGIN;
import static com.edouard.weatherapp.SearchCity.SEARCH;

public class AboutActivity extends AppCompatActivity
{

    private String displayedLocation;
    private GestureDetectorCompat mDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        mDetector = new GestureDetectorCompat(this, new MyGestureListener());

        TextView createdByView = (TextView) findViewById(R.id.createdByView);
        TextView myNameView = (TextView) findViewById(R.id.myNameView);
        TextView myEmailView = (TextView) findViewById(R.id.myEmailView);
        Typeface font1 = Typeface.createFromAsset(getAssets(), "fonts/Time Burner Bold.ttf");
        createdByView.setTypeface(font1);
        myNameView.setTypeface(font1);
        myEmailView.setTypeface(font1);
        Intent intent = getIntent();
        displayedLocation = intent.getStringExtra(DISPLAYED_LOCATION);
    }

    public void onEmailClick(View view)
    {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
//        intent.putExtra(Intent.EXTRA_EMAIL, new String[] {getResources().getString(R.string.my_email)});
//        intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.app_name));
//        intent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.sent_through_app));
//        startActivity(Intent.createChooser(intent, "Send Email"));
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

                // Right swipe
                if(-diff > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)
                {
                    AboutActivity.this.onRightSwipe();
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
