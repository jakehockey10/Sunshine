package com.example.jake.sunshine;

import android.annotation.TargetApi;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;


public class SensorsActivity extends ActionBarActivity implements SensorFragment.Callback {

    private final String LOG_TAG = SensorsActivity.class.getSimpleName();

    private boolean mTwoPane;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);
        if (findViewById(R.id.sensor_detail_container) != null) {

            // The detail container view will be present only in the large-screen layouts
            // (res/layout-sw600dp).  If this view is present, then the activity should be
            // in two-pane mode.
            mTwoPane = true;
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (savedInstanceState == null) {
                final SensorDetailFragment fragment = new SensorDetailFragment();
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.sensor_detail_container, fragment)
                        .commit();
            }
        } else {
            mTwoPane = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.sensor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(String sensor) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle args = new Bundle();
            args.putString(SensorDetailActivity.SENSOR_KEY, sensor);

            SensorDetailFragment fragment = new SensorDetailFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.sensor_detail_container, fragment)
                    .commit();
        } else {
            Intent intent = new Intent(this, SensorDetailActivity.class)
                    .putExtra(SensorDetailActivity.SENSOR_KEY, sensor);
            startActivity(intent);
        }
    }
}
