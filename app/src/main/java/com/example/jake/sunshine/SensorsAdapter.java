package com.example.jake.sunshine;

import android.content.Context;
import android.hardware.Sensor;
import android.widget.ArrayAdapter;

import java.util.ArrayList;

/**
 * {@link SensorsAdapter} exposes a list of sensors
 * from the available sensors on the given device
 * to a {@link android.widget.ListView}.
 */
public class SensorsAdapter extends ArrayAdapter {
    public SensorsAdapter(Context context, ArrayList<Sensor> sensors) {
        super(context, 0, sensors);
    }
}
