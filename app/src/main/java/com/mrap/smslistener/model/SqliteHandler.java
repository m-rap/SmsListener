package com.mrap.smslistener.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SqliteHandler {
    private static final String TAG = "SqliteHandler";

    public static SQLiteDatabase openDb(Context context, String path, String[] createSqls) {
        Log.d(TAG, "opening db");
        SQLiteOpenHelper sqLiteOpenHelper = new SQLiteOpenHelper(context,
                path, null, 1) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                for (String sql : createSqls) {
                    db.execSQL(sql);
                }
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            }
        };
        return sqLiteOpenHelper.getWritableDatabase();
    }
}
