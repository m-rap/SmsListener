package com.mrap.smslistener.model;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.Telephony;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

public abstract class SmsModel {

    private static final String TAG = "SmsModel";

    private final static String onCreateSql_0 = "" +
            "CREATE TABLE IF NOT EXISTS sms (\n" +
            "   sms_id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
            "   sms_number TEXT,\n" +
            "   sms_message TEXT,\n" +
            "   sms_timems INTEGER\n" +
            ")";

    public static SQLiteDatabase openDb_0(Context context) {
        SQLiteOpenHelper sqLiteOpenHelper = new SQLiteOpenHelper(context,
                context.getExternalFilesDir(null) + "/sms.db", null, 1) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL(onCreateSql_0);
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            }
        };
        return sqLiteOpenHelper.getReadableDatabase();
    }

    public static void insertSms_0(SQLiteDatabase smsDb, String number, String message, long timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("sms_number", number);
        contentValues.put("sms_message", message);
        contentValues.put("sms_timems", timestamp);
        smsDb.insert("sms", null, contentValues);
    }

    public static ArrayList<Sms> getSmss_0(SQLiteDatabase smsDb) {
        ArrayList<Sms> res = new ArrayList<>();

        Cursor c = smsDb.query("sms", new String[]{"sms_number", "sms_message", "sms_timems"},
                null, null, null, null, "sms_timems DESC");

        if (!c.moveToFirst()) {
            c.close();
            return res;
        }

        int addrIdx = c.getColumnIndex("sms_number");
        int bodyIdx = c.getColumnIndex("sms_message");
        int timeIdx = c.getColumnIndex("sms_timems");
        do {
            Sms row = new Sms() {{
                date = c.getLong(timeIdx);
                addr = c.getString(addrIdx);
                body = c.getString(bodyIdx);
                type = Telephony.Sms.MESSAGE_TYPE_INBOX;
            }};
            res.add(row);
        } while (c.moveToNext());

        c.close();
        return res;
    }

    public static ArrayList<Sms> getSmss_0(SQLiteDatabase smsDb, String number) {
        ArrayList<Sms> res = new ArrayList<>();

        Cursor c = smsDb.query("sms", new String[]{"sms_number", "sms_message", "sms_timems"},
                "sms_number='" + number + "'", null, null, null,
                "sms_timems DESC");
        if (!c.moveToFirst()) {
            c.close();
            return res;
        }

        int addrIdx = c.getColumnIndex("sms_number");
        int bodyIdx = c.getColumnIndex("sms_message");
        int timeIdx = c.getColumnIndex("sms_timems");
        do {
            Sms row = new Sms() {{
                addr = c.getString(addrIdx);
                body = c.getString(bodyIdx);
                date = c.getLong(timeIdx);
                type = Telephony.Sms.MESSAGE_TYPE_INBOX;
            }};
            res.add(row);
        } while (c.moveToNext());

        c.close();
        return res;
    }

    public static ArrayList<Sms> getLastSmss_0(SQLiteDatabase smsDb) {
        ArrayList<Sms> res = new ArrayList<>();

        Cursor c = smsDb.rawQuery("" +
                "SELECT s2.* FROM (\n" +
                "   SELECT sms_number, MAX(sms_timems) as max_timems FROM sms GROUP BY sms_number\n" +
                ") s1 INNER JOIN sms s2 ON s1.sms_number=s2.sms_number AND \n" +
                "s1.max_timems=s2.sms_timems ORDER BY s2.sms_timems DESC", null);

        if (!c.moveToFirst()) {
            c.close();
            return res;
        }

        int addrIdx = c.getColumnIndex("sms_number");
        int bodyIdx = c.getColumnIndex("sms_message");
        int timeIdx = c.getColumnIndex("sms_timems");
        do {
            Sms row = new Sms() {{
                addr = c.getString(addrIdx);
                body = c.getString(bodyIdx);
                date = c.getLong(timeIdx);
                type = Telephony.Sms.MESSAGE_TYPE_INBOX;
            }};
            res.add(row);
        } while (c.moveToNext());

        c.close();

        return res;
    }

    private static boolean listContainsAddr(ArrayList<Sms> list, String addr) {
        for (Sms sms : list) {
            if (sms.addr.equals(addr)) {
                return true;
            }
        }
        return false;
    }
}
