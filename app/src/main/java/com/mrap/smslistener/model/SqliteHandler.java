package com.mrap.smslistener.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SqliteHandler {
    private static final String TAG = "SqliteHandler";

    public static SQLiteDatabase openDb(Context context, String path, String createSql) {
        Log.d(TAG, "opening db");
        SQLiteOpenHelper sqLiteOpenHelper = new SQLiteOpenHelper(context,
                path, null, 1) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL(createSql);
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            }
        };
        return sqLiteOpenHelper.getWritableDatabase();
    }
}
