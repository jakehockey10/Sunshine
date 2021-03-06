/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.jake.sunshine.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.preference.PreferenceManager;

import com.example.jake.sunshine.R;
import com.example.jake.sunshine.data.WeatherContract;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Utility {
    public static String getPreferredLocation(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_location_key),
                context.getString(R.string.pref_location_default));
    }

    public static boolean isMetric(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_units_key),
                context.getString(R.string.pref_units_metric))
                .equals(context.getString(R.string.pref_units_metric));
    }

    public static String formatTemperature(Context context, double temperature) {
        // Data stored in Celsius by default.  If user prefers to see in Fahrenheit, convert
        // the values here.
        String suffix = "\u00B0";
        if (!isMetric(context)) {
            temperature = (temperature * 1.8) + 32;
        }

        // For presentation, assume the user doesn't care about tenths of a degree.
        return String.format(context.getString(R.string.format_temperature), temperature);
    }

    static String formatDate(String dateString) {
        Date date = WeatherContract.getDateFromDb(dateString);
        return DateFormat.getDateInstance().format(date);
    }

    // Format used for storing dates in the database.  ALso used for converting those strings
    // back into date objects for comparison/processing.
    public static final String DATE_FORMAT = "yyyyMMdd";

    /**
     * Helper method to convert the database representation of the date into something to display
     * to users.  As classy and polished a user experience as "20140102" is, we can do better.
     *
     * @param context Context to use for resource localization
     * @param dateStr The db formatted date string, expected to be of the form specified
     *                in Utility.DATE_FORMAT
     * @return a user-friendly representation of the date.
     */
    public static String getFriendlyDayString(Context context, String dateStr) {
        // The day string for forecast uses the following logic:
        // For today: "Today, June 8"
        // For tomorrow:  "Tomorrow"
        // For the next 5 days: "Wednesday" (just the day name)
        // For all days after that: "Mon Jun 8"

        Date todayDate = new Date();
        String todayStr = WeatherContract.getDbDateString(todayDate);
        Date inputDate = WeatherContract.getDateFromDb(dateStr);

        // If the date we're building the String for is today's date, the format
        // is "Today, June 24"
        if (todayStr.equals(dateStr)) {
            String today = context.getString(R.string.today);
            int formatId = R.string.format_full_friendly_date;
            return String.format(context.getString(
                    formatId,
                    today,
                    getFormattedMonthDay(context, dateStr)));
        } else {
            Calendar cal = Calendar.getInstance();
            cal.setTime(todayDate);
            cal.add(Calendar.DATE, 7);
            String weekFutureString = WeatherContract.getDbDateString(cal.getTime());

            if (dateStr.compareTo(weekFutureString) < 0) {
                // If the input date is less than a week in the future, just return the day name.
                return getDayName(context, dateStr);
            } else {
                // Otherwise, use the form "Mon Jun 3"
                SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
                return shortenedDateFormat.format(inputDate);
            }
        }
    }

    /**
     * Given a day, returns just the name to use for that day.
     * E.g "today", "tomorrow", "wednesday".
     *
     * @param context Context to use for resource localization
     * @param dateStr The db formatted date string, expected to be of the form specified
     *                in Utility.DATE_FORMAT
     * @return
     */
    public static String getDayName(Context context, String dateStr) {
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(Utility.DATE_FORMAT);
        try {
            Date inputDate = dbDateFormat.parse(dateStr);
            Date todayDate = new Date();
            // If the date is today, return the localized version of "Today" instead of the actual
            // day name.
            if (WeatherContract.getDbDateString(todayDate).equals(dateStr)) {
                return context.getString(R.string.today);
            } else {
                // If the date is set for tomorrow, the format is "Tomorrow".
                Calendar cal = Calendar.getInstance();
                cal.setTime(todayDate);
                cal.add(Calendar.DATE, 1);
                Date tomorrowDate = cal.getTime();
                if (WeatherContract.getDbDateString(tomorrowDate).equals(
                        dateStr)) {
                    return context.getString(R.string.tomorrow);
                } else {
                    // Otherwise, the format is just the day of the week (e.g "Wednesday".
                    SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
                    return dayFormat.format(inputDate);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
            // It couldn't process the date correctly.
            return "";
        }
    }

    /**
     * Converts db date format to the format "Month day", e.g "June 24".
     * @param context Context to use for resource localization
     * @param dateStr The db formatted date string, expected to be of the form specified
     *                in Utility.DATE_FORMAT
     * @return The day in the form of a string formatted "December 6"
     */
    public static String getFormattedMonthDay(Context context, String dateStr) {
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(Utility.DATE_FORMAT);
        try {
            Date inputDate = dbDateFormat.parse(dateStr);
            SimpleDateFormat monthDayFormat = new SimpleDateFormat("MMMM dd");
            String monthDayString = monthDayFormat.format(inputDate);
            return monthDayString;
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getFormattedWind(Context context, float windSpeed, float degrees) {
        int windFormat;
        if (Utility.isMetric(context)) {
            windFormat = R.string.format_wind_kmh;
        } else {
            windFormat = R.string.format_wind_mph;
            windSpeed = .621371192237334f * windSpeed;
        }

        // From wind direction in degrees, determine compass direction as a string (e.g NW)
        // You know what's fun, writing really long if/else statements with tons of possible
        // conditions.  Seriously, try it!
        String direction = "Unknown";
        if (degrees >= 337.5 || degrees < 22.5) {
            direction = "N";
        } else if (degrees >= 22.5 && degrees < 67.5) {
            direction = "NE";
        } else if (degrees >= 67.5 && degrees < 112.5) {
            direction = "E";
        } else if (degrees >= 112.5 && degrees < 157.5) {
            direction = "SE";
        } else if (degrees >= 157.5 && degrees < 202.5) {
            direction = "S";
        } else if (degrees >= 202.5 && degrees < 247.5) {
            direction = "SW";
        } else if (degrees >= 247.5 && degrees < 292.5) {
            direction = "W";
        } else if (degrees >= 292.5 || degrees < 22.5) {
            direction = "NW";
        }
        return String.format(context.getString(windFormat), windSpeed, direction);
    }

    /**
     * Helper method to provide the icon resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding icon. -1 if no relation is found.
     */
    public static int getIconResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_cloudy;
        }
        return -1;
    }

    /**
     * Helper method to provide the art resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding image. -1 if no relation is found.
     */
    public static int getArtResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.art_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.art_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.art_rain;
        } else if (weatherId == 511) {
            return R.drawable.art_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.art_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.art_rain;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.art_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.art_storm;
        } else if (weatherId == 800) {
            return R.drawable.art_clear;
        } else if (weatherId == 801) {
            return R.drawable.art_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.art_clouds;
        }
        return -1;
    }

    public static int getSensorTypeFromSensorString(String type) {
        switch (type) {
            case Sensor.STRING_TYPE_ACCELEROMETER:
                return Sensor.TYPE_ACCELEROMETER;
            case Sensor.STRING_TYPE_AMBIENT_TEMPERATURE:
                return Sensor.TYPE_AMBIENT_TEMPERATURE;
            case Sensor.STRING_TYPE_GAME_ROTATION_VECTOR:
                return Sensor.TYPE_GAME_ROTATION_VECTOR;
            case Sensor.STRING_TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                return Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR;
            case Sensor.STRING_TYPE_GRAVITY:
                return Sensor.TYPE_GRAVITY;
            case Sensor.STRING_TYPE_GYROSCOPE:
                return Sensor.TYPE_GYROSCOPE;
            case Sensor.STRING_TYPE_GYROSCOPE_UNCALIBRATED:
                return Sensor.TYPE_GYROSCOPE_UNCALIBRATED;
            case Sensor.STRING_TYPE_HEART_RATE:
                return Sensor.TYPE_HEART_RATE;
            case Sensor.STRING_TYPE_LIGHT:
                return Sensor.TYPE_LIGHT;
            case Sensor.STRING_TYPE_LINEAR_ACCELERATION:
                return Sensor.TYPE_LINEAR_ACCELERATION;
            case Sensor.STRING_TYPE_MAGNETIC_FIELD:
                return Sensor.TYPE_MAGNETIC_FIELD;
            case Sensor.STRING_TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                return Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED;
            case Sensor.STRING_TYPE_ORIENTATION:
                return Sensor.TYPE_ORIENTATION;
            case Sensor.STRING_TYPE_PRESSURE:
                return Sensor.TYPE_PRESSURE;
            case Sensor.STRING_TYPE_PROXIMITY:
                return Sensor.TYPE_PROXIMITY;
            case Sensor.STRING_TYPE_RELATIVE_HUMIDITY:
                return Sensor.TYPE_RELATIVE_HUMIDITY;
            case Sensor.STRING_TYPE_ROTATION_VECTOR:
                return Sensor.TYPE_ROTATION_VECTOR;
            case Sensor.STRING_TYPE_SIGNIFICANT_MOTION:
                return Sensor.TYPE_SIGNIFICANT_MOTION;
            case Sensor.STRING_TYPE_STEP_COUNTER:
                return Sensor.TYPE_STEP_COUNTER;
            case Sensor.STRING_TYPE_STEP_DETECTOR:
                return Sensor.TYPE_STEP_DETECTOR;
            case Sensor.STRING_TYPE_TEMPERATURE:
                return Sensor.TYPE_TEMPERATURE;
            default:
                return -1;
        }
    }

    public static String getSensorTypeStringFromInt(int type) {
        switch (type) {
            case Sensor.TYPE_ACCELEROMETER:
                return Sensor.STRING_TYPE_ACCELEROMETER;
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                return Sensor.STRING_TYPE_AMBIENT_TEMPERATURE;
            case Sensor.TYPE_GAME_ROTATION_VECTOR:
                return Sensor.STRING_TYPE_GAME_ROTATION_VECTOR;
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                return Sensor.STRING_TYPE_GEOMAGNETIC_ROTATION_VECTOR;
            case Sensor.TYPE_GRAVITY:
                return Sensor.STRING_TYPE_GRAVITY;
            case Sensor.TYPE_GYROSCOPE:
                return Sensor.STRING_TYPE_GYROSCOPE;
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                return Sensor.STRING_TYPE_GYROSCOPE_UNCALIBRATED;
            case Sensor.TYPE_HEART_RATE:
                return Sensor.STRING_TYPE_HEART_RATE;
            case Sensor.TYPE_LIGHT:
                return Sensor.STRING_TYPE_LIGHT;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                return Sensor.STRING_TYPE_LINEAR_ACCELERATION;
            case Sensor.TYPE_MAGNETIC_FIELD:
                return Sensor.STRING_TYPE_MAGNETIC_FIELD;
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                return Sensor.STRING_TYPE_MAGNETIC_FIELD_UNCALIBRATED;
            case Sensor.TYPE_ORIENTATION:
                return Sensor.STRING_TYPE_ORIENTATION;
            case Sensor.TYPE_PRESSURE:
                return Sensor.STRING_TYPE_PRESSURE;
            case Sensor.TYPE_PROXIMITY:
                return Sensor.STRING_TYPE_PROXIMITY;
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                return Sensor.STRING_TYPE_RELATIVE_HUMIDITY;
            case Sensor.TYPE_ROTATION_VECTOR:
                return Sensor.STRING_TYPE_ROTATION_VECTOR;
            case Sensor.TYPE_SIGNIFICANT_MOTION:
                return Sensor.STRING_TYPE_SIGNIFICANT_MOTION;
            case Sensor.TYPE_STEP_COUNTER:
                return Sensor.STRING_TYPE_STEP_COUNTER;
            case Sensor.TYPE_STEP_DETECTOR:
                return Sensor.STRING_TYPE_STEP_DETECTOR;
            case Sensor.TYPE_TEMPERATURE:
                return Sensor.STRING_TYPE_TEMPERATURE;
            default:
                return "";
        }
    }

    public static int getValueCountForSensor(String type) {
        int typeInt = getSensorTypeFromSensorString(type);
        return getValueCountForSensor(typeInt);
    }

    public static int getValueCountForSensor(int type) {
        switch (type) {
            case Sensor.TYPE_ACCELEROMETER:
                return 3;
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                return 1;
            case Sensor.TYPE_GAME_ROTATION_VECTOR:
                return 3;
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                return 3;
            case Sensor.TYPE_GRAVITY:
                return 3;
            case Sensor.TYPE_GYROSCOPE:
                return 3;
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                return 6;
            case Sensor.TYPE_HEART_RATE:
                return -1;
            case Sensor.TYPE_LIGHT:
                return 1;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                return 3;
            case Sensor.TYPE_MAGNETIC_FIELD:
                return 3;
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                return 6;
            case Sensor.TYPE_ORIENTATION: // Deprecated Android 2.2 (API Level 8).
                return 3;
            case Sensor.TYPE_PRESSURE:
                return 1;
            case Sensor.TYPE_PROXIMITY:
                return 1;
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                return 1;
            case Sensor.TYPE_ROTATION_VECTOR:
                return 4;
            case Sensor.TYPE_SIGNIFICANT_MOTION:
                return 0;
            case Sensor.TYPE_STEP_COUNTER:
                return 1;
            case Sensor.TYPE_STEP_DETECTOR:
                return 0;
            case Sensor.TYPE_TEMPERATURE: // Deprecated in Android 4.0 (API Level 14).
                return 1;
            default:
                return -1;
        }
    }
    
    public static String convertInputeStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;
    }
}
