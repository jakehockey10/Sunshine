package com.example.jake.sunshine;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A placeholder fragment containing a simple view.
 */
public class SensorDetailFragment extends Fragment {

    private static final String LOG_TAG = SensorDetailFragment.class.getSimpleName();

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
            // do stuff with the arguments (see WeatherDetailFragment.java)
        }

        if (savedInstanceState != null) {
            // do stuff with savedInstanceState (see WeatherDetailFragment.java)
        }

        View rootView = inflater.inflate(R.layout.fragment_detail_sensor, container, false);
        // find the views like in WeatherDetailFragment.java.
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Bundle arguments = getArguments();
        // check for arguments and particular key like in WeatherDetailFragment.java
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
}
