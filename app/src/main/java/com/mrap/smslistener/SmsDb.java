package com.mrap.smslistener;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class SmsDb {
    private SQLiteDatabase db = null;
    private static String TAG = "SmsDb";
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

    public void openDb(Context context) {
        SQLiteOpenHelper sqLiteOpenHelper = new SQLiteOpenHelper(context, context.getExternalFilesDir(null) + "/sms.db", null, 1) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL("" +
                        "CREATE TABLE IF NOT EXISTS sms (\n" +
                        "   sms_id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                        "   sms_number TEXT,\n" +
                        "   sms_message TEXT,\n" +
                        "   sms_timems INTEGER\n" +
                        ")");
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            }
        };
        db = sqLiteOpenHelper.getWritableDatabase();
    }

    public void insertSms(String number, String message, long timestamp) {
        if (db == null) {
            Log.e(TAG, "db is not opened");
            return;
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put("sms_number", number);
        contentValues.put("sms_message", message);
        contentValues.put("sms_timems", timestamp);
        db.insert("sms", null, contentValues);
    }

    public String[][] getSmss() {
        ArrayList<String[]> res = new ArrayList<>();
        if (db == null) {
            Log.e(TAG, "db is not opened");
            return res.toArray(new String[res.size()][]);
        }

        Cursor c = db.query("sms", new String[]{"sms_number", "sms_message", "sms_timems"},
                null, null, null, null, "sms_timems DESC");

        if (!c.moveToFirst()) {
            c.close();
            return res.toArray(new String[res.size()][]);
        }

        int numIdx = c.getColumnIndex("sms_number");
        int msgIdx = c.getColumnIndex("sms_message");
        int timeIdx = c.getColumnIndex("sms_timems");
        do {
            String[] row = {
                    c.getString(numIdx),
                    c.getString(msgIdx),
                    sdf.format(new Date(c.getLong(timeIdx)))};
            res.add(row);
        } while (c.moveToNext());

        c.close();
        return res.toArray(new String[res.size()][]);
    }

    public void close() {
        if (db == null) {
            Log.e(TAG, "db is not opened");
            return;
        }

        db.close();
    }
}
