package com.example.jake.sunshine;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

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

        mSensorManager = (SensorManager) getActivity().getSystemService(getActivity().SENSOR_SERVICE);
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
        linearLayout.addView(mTextViews[i-1]);

        return mRootView;
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.sensordetailfragment, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            // see WeatherDetailFragment.java
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
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        mTextViews[mTextViews.length - 1].setText("Accuracy: " + accuracy);
    }
}
