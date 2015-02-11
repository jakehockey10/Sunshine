package com.example.jake.sunshine;

import android.content.Context;
import android.hardware.Sensor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * {@link SensorsAdapter} exposes a list of sensors
 * from the available sensors on the given device
 * to a {@link android.widget.ListView}.
 */
public class SensorsAdapter extends ArrayAdapter<Sensor> {

    public SensorsAdapter(Context context, ArrayList<Sensor> sensors) {
        super(context, 0, sensors);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Sensor sensor = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_sensor, parent, false);
        }
        // Lookup view for data population
        TextView sensorName = (TextView) convertView.findViewById(R.id.list_item_sensor_name);
        // Populate the data into the template view using the data object
        sensorName.setText(sensor.getName());
        // Return the completed view to render on screen
        return convertView;
    }
}
