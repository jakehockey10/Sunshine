package com.example.jake.sunshine;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jake.sunshine.data.SensorDataHelper;
import com.example.jake.sunshine.data.SensorDataProvider;
import com.example.jake.sunshine.util.Utility;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;

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
    private static final int VALS_MAX = 100000;
    
    private ArrayList<SensorEvent> mSensorEvents;

    private static final String SESSION_ID = "SessionID";

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

        // Restore preferences
        SharedPreferences preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        int launchCount = preferences.getInt("launchCount", 0);
        preferences.edit().putInt("launchCount", ++launchCount).commit();

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
        } else if (id == R.id.action_save_data) {
            mSensorManager.unregisterListener(this);
            new SensorEventLoggerTask().execute(mSensorEvents);
        } else if (id == R.id.action_send_data) {
            new SendSensorDataTask().execute();
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
        
        // Retrieve the send data menu item
        MenuItem sendDataMenuItem = menu.findItem(R.id.action_send_data);
        
        // Retrieve the save data menu item
        MenuItem saveDataMenuItem = menu.findItem(R.id.action_save_data);

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
        
        if (mSensorEvents == null) mSensorEvents = new ArrayList<>();
        if (mSensorEvents.size() < VALS_MAX) {
            mSensorEvents.add(event);
        } else {
            mSensorManager.unregisterListener(this);
            new SensorEventLoggerTask().execute(mSensorEvents);
        }
        
//        // Populate the chart
//        if (mVals != null) {
//            ArrayList<LineDataSet> lineDataSets = new ArrayList<LineDataSet>();
//            int valueCountForSensor = Utility.getValueCountForSensor(mSensor.getType());
//            for (int i = 0; i < valueCountForSensor; i++) {
//                if (mVals.get(i).size() >= VALS_MAX) {
//                    mVals.get(i).remove(0);
//                }
//                // casting event.timestamp to an int from a long could maybe suffer
//                // if timestamp isn't properly set.
//                mVals.get(i).add(new Entry(event.values[i], (int) event.timestamp));
//                // TODO: Give better labels to these data sets.
//                lineDataSets.add(new LineDataSet(mVals.get(i), "Value " + i + 1));
//            }
//
//            LineData data = new LineData(new ArrayList<String>(lineDataSets.size()), lineDataSets);
//            mChart.setData(data);
//        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        mTextViews[mTextViews.length - 1].setText("Accuracy: " + accuracy);
    }

    private class SensorEventLoggerTask extends AsyncTask<List<SensorEvent>, Void, Void> {
        @Override
        protected void onPreExecute() {
            Toast toast = Toast.makeText(getActivity(), "Saving...", Toast.LENGTH_SHORT);
            toast.show();
            
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Toast toast = Toast.makeText(getActivity(), "Done saving the data.  You can send now.", Toast.LENGTH_LONG);
            toast.show();
            
            mSensorEvents = new ArrayList<>();

            super.onPostExecute(aVoid);
        }

        @Override
        protected Void doInBackground(List<SensorEvent>... events) {
            for (int i = 0; i < events.length; i++) {
                SensorEvent event = events[0].get(i);
                String value = Arrays.toString(event.values);
                long timestamp = (new Date()).getTime() + (event.timestamp - System.nanoTime()) / 100000L;
                ContentValues values = new ContentValues();
                SharedPreferences preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
                int launchCount = preferences.getInt("launchCount", 0);
                values.put(SensorDataHelper.COL_SESSION_ID, launchCount);
                values.put(SensorDataHelper.COL_ACCURACY, event.accuracy);
                values.put(SensorDataHelper.COL_NAME, mSensorName);
                // TODO: get the email from the user
                values.put(SensorDataHelper.COL_EMAIL, "jakehockey10@gmail.com");
                values.put(SensorDataHelper.COL_TIMESTAMP, timestamp);
                values.put(SensorDataHelper.COL_VALUE, value);
                getActivity().getContentResolver().insert(SensorDataProvider.CONTENT_URI, values);
            }
            return null;
        }
    }

    private class SendSensorDataTask extends AsyncTask<String, String, String> {
        private List<HashMap<String, String>> mData = null; // post data

        /**
         * constructor
         */
//        public SendSensorDataTask(List<HashMap<String, String>> data) {
//            mData = data;
//        }
        public SendSensorDataTask(){}

        @Override
        protected void onPreExecute() {
            Toast toast = Toast.makeText(getActivity(), "Sending...", Toast.LENGTH_SHORT);
            toast.show();

            super.onPreExecute();
        }

        /**
         * background
         */
        @Override
        protected String doInBackground(String... params) {

            String result = "";
            Cursor cursor = getActivity().getContentResolver().query(SensorDataProvider.CONTENT_URI, null, null, null, null);

            JSONArray data = new JSONArray();
            cursor.moveToFirst();
            while(!cursor.isAfterLast()){
                JSONObject rowObject = new JSONObject();
                JSONObject innerObject = new JSONObject();

                try {
                    innerObject.put("accuracy", parseInt(cursor.getString(cursor.getColumnIndex(SensorDataHelper.COL_ACCURACY))));
                    innerObject.put("sensor", cursor.getString(cursor.getColumnIndex(SensorDataHelper.COL_NAME)));
                    innerObject.put("email", cursor.getString(cursor.getColumnIndex(SensorDataHelper.COL_EMAIL)));
                    innerObject.put("timestamp", parseLong(cursor.getString(cursor.getColumnIndex(SensorDataHelper.COL_TIMESTAMP))));
                    innerObject.put("values", cursor.getString(cursor.getColumnIndex(SensorDataHelper.COL_VALUE)));
                    String values = innerObject.get("values").toString();
                    String substring = values.substring(1, values.length() - 1);
                    String[] split = substring.split(", ");
                    innerObject.put("values", new JSONArray(split));
                    rowObject.put("sensor_event", innerObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost("https://pacific-island-4200.herokuapp.com/sensor_events");
                String json = rowObject.toString();

                try {
                    StringEntity se = new StringEntity(json);
                    httpPost.setEntity(se);
                    httpPost.setHeader("Accept", "application/json");
                    httpPost.setHeader("Content-type", "application/json");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                try {
                    HttpResponse httpResponse = httpClient.execute(httpPost);
                    InputStream inputStream = httpResponse.getEntity().getContent();
                    if (inputStream != null)
                        result = Utility.convertInputeStreamToString(inputStream);
                    else
                        result = "Did not work!";

                } catch (Exception e) {
                    e.printStackTrace();
                }

//                data.put(rowObject);
                cursor.moveToNext();
            }

            cursor.close();

            return result;
        }

        /**
         * on getting result
         */
        @Override
        protected void onPostExecute(String result) {
            Toast toast = Toast.makeText(getActivity(), "Attempted to send with result: " + result, Toast.LENGTH_LONG);
            toast.show();

            super.onPostExecute(result);
        }
    }
}
