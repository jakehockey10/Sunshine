package com.example.jake.sunshine;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class SensorDetailFragment extends Fragment implements SensorEventListener {

    private static final String LOG_TAG = SensorDetailFragment.class.getSimpleName();

    private TextView mNameView;
    private String mSensorName;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private View mRootView;
    private TextView[] mTextViews;

    private ShareActionProvider mShareActionProvider;
    private LineChart mChart;
    private ArrayList<ArrayList<Entry>> mVals;
    private static final int VALS_MAX = 100;

    public SensorDetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // from WeatherDetailFragment.java:
        // outState.putString(LOCATION_KEY, mLocation);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments != null) {
            mSensorName = arguments.getString(SensorDetailActivity.SENSOR_KEY);
        }

        if (savedInstanceState != null) {
            // do stuff with savedInstanceState (see WeatherDetailFragment.java)
        }

        // Figure out what sensor is supposed to show.
        mSensorManager = (SensorManager) getActivity().getSystemService(getActivity().SENSOR_SERVICE);
        if (mSensorName == null) {
            // Set a default if the SensorDetailFragment is being created
            // without the user having selected a sensor yet (tablets have
            // two panes for the SensorsActivity and the SensorDetailActivity
            // (with their respective fragments.
            mSensorName = mSensorManager.getSensorList(Sensor.TYPE_ALL).get(0).getName();
        }
        mSensor = mSensorManager.getDefaultSensor(Utility.getSensorTypeFromSensorString(mSensorName));

        mRootView = inflater.inflate(R.layout.fragment_detail_sensor, container, false);
        // find the views like in WeatherDetailFragment.java.
        TextView nameView = (TextView) mRootView.findViewById(R.id.detail_sensor_textview);
        nameView.setText(mSensorName);

        LinearLayout linearLayout = (LinearLayout) mRootView.findViewById(R.id.fragment_sensor_linear_layout);

        // Add one more textview for accuracy reading ( + 1 )
        mTextViews = new TextView[Utility.getValueCountForSensor(mSensor.getType()) + 1]; // create an empty array;

        int i;
        for (i = 0; i < mTextViews.length; i++) {
            // create a new textview
            final TextView rowTextView = new TextView(getActivity());

            // set a tag so that we can refer to these textviews
            // in sensor event listeners.
            // rowTextView.setTag(i);

            // add the textview to the linearlayout
            linearLayout.addView(rowTextView);

            // save a reference to the textview for later
            mTextViews[i] = rowTextView;
        }

        // Add TextView for accuracy
        mTextViews[i-1] = new TextView(getActivity());
        linearLayout.addView(mTextViews[i - 1]);

        return mRootView;
    }

    public void initializeChartObjects() {
        LinearLayout linearLayout = (LinearLayout) getActivity().findViewById(R.id.fragment_sensor_linear_layout);
        mChart = new LineChart(getActivity());
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.MATCH_PARENT);
        mChart.setLayoutParams(params);
        linearLayout.addView(mChart);
//        mChart = (LineChart) getActivity().findViewById(R.id.chart);
        int valueCountForSensor = Utility.getValueCountForSensor(mSensor.getType());
        mVals = new ArrayList<ArrayList<Entry>>(valueCountForSensor);
        for (int i = 0; i < valueCountForSensor; i++) {
            mVals.add(new ArrayList<Entry>());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Bundle arguments = getArguments();
        // check for arguments and particular key like in WeatherDetailFragment.java
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add_chart) {
            initializeChartObjects();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.sensordetailfragment, menu);

        // Retrieve the share menu item
        MenuItem shareMenuItem = menu.findItem(R.id.action_share);
        
        // Retrieve the chart menu item
        MenuItem chartMenuItem = menu.findItem(R.id.action_add_chart);
        

        // Get the provider and hold onto it to set/change the share intent.
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareMenuItem);

        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareSensorIntent());
        }
    }

    private Intent createShareSensorIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Using my " + mSensorName + " sensor on my Android device!");
        return shareIntent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {

        }

        Bundle arguments = getArguments();
        // see WeatherDetailFragment.java
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Accuracy is handled in the onAccuracy Changed ( - 1 )
        for (int i = 0; i < mTextViews.length - 1; i++) {
            mTextViews[i].setText("Value " + i + ": " + event.values[i]);
        }

        // Populate the chart
        if (mVals != null) {
            ArrayList<LineDataSet> lineDataSets = new ArrayList<LineDataSet>();
            int valueCountForSensor = Utility.getValueCountForSensor(mSensor.getType());
            for (int i = 0; i < valueCountForSensor; i++) {
                if (mVals.get(i).size() >= VALS_MAX) {
                    mVals.get(i).remove(0);
                }
                // casting event.timestamp to an int from a long could maybe suffer
                // if timestamp isn't properly set.
                mVals.get(i).add(new Entry(event.values[i], (int) event.timestamp));
                // TODO: Give better labels to these data sets.
                lineDataSets.add(new LineDataSet(mVals.get(i), "Value " + i + 1));
            }

            LineData data = new LineData(new ArrayList<String>(lineDataSets.size()), lineDataSets);
            mChart.setData(data);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        mTextViews[mTextViews.length - 1].setText("Accuracy: " + accuracy);
    }
}
