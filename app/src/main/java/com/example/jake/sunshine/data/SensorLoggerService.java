package com.example.jake.sunshine.data;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.IBinder;

import java.util.Arrays;
import java.util.Date;

/**
* Created by jake on 3/2/15.
*/
public class SensorLoggerService extends Service implements SensorEventListener {
    private static final String DEBUG_TAG = "SensorLoggerService";

    private SensorManager sensorManager = null;
    private Sensor sensor = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // grab the values and timestamp -- of the main thread
        new SensorEventLoggerTask().execute(event);
        // stop the sensor and service
        stopSelf();
    }

    private class SensorEventLoggerTask extends AsyncTask<SensorEvent, Void, Void> {
        @Override
        protected Void doInBackground(SensorEvent... events) {
            SensorEvent event = events[0];

            String value = Arrays.toString(event.values);
            long timestamp = (new Date()).getTime() + (event.timestamp - System.nanoTime()) / 1000000L;
            
            // log the value
            ContentValues values = new ContentValues();
            values.put(SensorDataHelper.COL_VALUE, value);
            values.put(SensorDataHelper.COL_TIMESTAMP, timestamp);
            getContentResolver().insert(SensorDataProvider.CONTENT_URI, values);
            return null;
        }
    }
}
