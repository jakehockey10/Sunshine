package com.example.jake.sunshine.data;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

/**
 * Created by jake on 3/2/15.
 */
public class SensorDataProvider extends ContentProvider {
    private static final String DEBUG_TAG = "SensorProvider";
    private SensorDataHelper sensorDataHelper;
    
    public static final int SENSORDATA = 100;
    public static final int SENSORDATA_ID = 110;
    
    private static final String AUTHORITY = "com.example.jake.sunshine.data.SensorDataProvider";
    private static final String BASE_PATH = "sensordata";
    
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
            + "/" + BASE_PATH);
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
            + "/sensorlog";
    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
            +  "/sensorlog";
    
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, SENSORDATA);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", SENSORDATA_ID);
    }
    
    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, 
                        String sortOrder) {
//        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
//        queryBuilder.setTables(SensorDataHelper.TABLE_NAME);
//        
//        int uriType = sURIMatcher.match(uri);
//        switch (uriType) {
//            case SENSORDATA_ID:
//                queryBuilder.appendWhere(SensorDataHelper.COL_ID + "=" + uri.getLastPathSegment());
//                break;
//            case SENSORDATA:
//                // no filter
//                break;
//            default:
//                throw new IllegalArgumentException("unknown URI");
//        }
//        
//        Cursor cursor = queryBuilder.query(getDatabase(false), projection, selection, selectionArgs,
//                null, null, sortOrder);
//        cursor.setNotificationUri(getContext().getContentResolver(), uri);
//        return cursor;
        Cursor cursor = getDatabase(false).rawQuery("select * from " + SensorDataHelper.TABLE_NAME, null);
        String hello = "hello";
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        if (uriType != SENSORDATA) {
            throw new IllegalArgumentException("Invalid URI for insert");
        }
        SQLiteDatabase sqlDB = getDatabase(true);
        try {
            long newID = sqlDB.insertOrThrow(SensorDataHelper.TABLE_NAME, null, values);
            if (newID > 0) {
                Uri newUri = ContentUris.withAppendedId(uri, newID);
                getContext().getContentResolver().notifyChange(uri, null);
                return newUri;
            } else {
                throw new SQLException("Failed to insert row into " + uri);
            }
        }
        catch (SQLiteConstraintException e) {
            Log.w(DEBUG_TAG, "Warning: Ignoring constraint failure.");
        }
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
    
    private SQLiteDatabase getDatabase(boolean writable) {
        if (sensorDataHelper == null) {
            sensorDataHelper = new SensorDataHelper(getContext());
        }
        
        return (writable ? sensorDataHelper.getWritableDatabase() 
                : sensorDataHelper.getReadableDatabase());
    }
}
