package com.example.jake.sunshine.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by jake on 3/2/15.
 */
public class SensorDataHelper extends SQLiteOpenHelper {
    private static final String DEBUG_TAG = "SensorDataHelper";

    private static final String DATABASE_NAME = "sensor_data.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "table_sensor_data";
    public static final String COL_ID = "_id";
    public static final String COL_SESSION_ID = "session_id";
    public static final String COL_NAME = "name";
    public static final String COL_TIMESTAMP = "timestamp";
    public static final String COL_VALUE = "value";
    public static final String COL_ACCURACY = "accuracy";
    public static final String COL_EMAIL = "email";
    
    private static final String DB_SCHEMA = "CREATE TABLE " + TABLE_NAME + "("
            + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_SESSION_ID + " INTEGER FOREIGN KEY, "
            + COL_NAME + " STRING, "
            + COL_TIMESTAMP + " INTEGER NOT NULL, "
            + COL_VALUE + " REAL, "
            + COL_ACCURACY + " INTEGER NOT NULL, "
            + COL_EMAIL + " STRING NOT NULL);";

    SensorDataHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DB_SCHEMA);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(DEBUG_TAG, "Warning: Dropping all tables; data migration not supported");
        db.execSQL("DROP TABEL IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
