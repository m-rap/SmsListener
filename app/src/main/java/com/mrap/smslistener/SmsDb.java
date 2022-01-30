package com.mrap.smslistener;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.Telephony;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class SmsDb {
    private SQLiteDatabase db = null;
    private static String TAG = "SmsDb";
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
    private Context context;

    public void openDb(Context context) {
        this.context = context;
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

    public ArrayList<String[]> getSmss() {
        ArrayList<String[]> res = new ArrayList<>();
        if (db == null) {
            Log.e(TAG, "db is not opened");
            return res;
        }

        Cursor c = db.query("sms", new String[]{"sms_number", "sms_message", "sms_timems"},
                null, null, null, null, "sms_timems DESC");

        if (!c.moveToFirst()) {
            c.close();
            return res;
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
        return res;
    }

    public ArrayList<String[]> getSmss(String number) {
        ArrayList<String[]> res = new ArrayList<>();

        Cursor c = db.query("sms", new String[]{"sms_number", "sms_message", "sms_timems"},
                "sms_number='" + number + "'", null, null, null, null);
        if (!c.moveToFirst()) {
            c.close();
            return res;
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
        return res;
    }

    public void getSmssFromContentResolver(Context context) {
        ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(Telephony.Sms.CONTENT_URI, null, null, null, null);
        int totalSMS = 0;
        if (c == null) {
            return;
        }
        totalSMS = c.getCount();
        if (!c.moveToFirst()) {
            c.close();
            return;
        }

        int idxDate = c.getColumnIndexOrThrow(Telephony.Sms.DATE);
        int idxAddr = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS);
        int idxBody = c.getColumnIndexOrThrow(Telephony.Sms.BODY);
        int idxType = c.getColumnIndexOrThrow(Telephony.Sms.TYPE);

        for (int j = 0; j < totalSMS; j++) {
            String smsDate = c.getString(idxDate);
            String number = c.getString(idxAddr);
            String body = c.getString(idxBody);
            Date dateFormat = new Date(Long.valueOf(smsDate));
            String type;
            switch (Integer.parseInt(c.getString(idxType))) {
                case Telephony.Sms.MESSAGE_TYPE_INBOX:
                    type = "inbox";
                    break;
                case Telephony.Sms.MESSAGE_TYPE_SENT:
                    type = "sent";
                    break;
                case Telephony.Sms.MESSAGE_TYPE_OUTBOX:
                    type = "outbox";
                    break;
                default:
                    break;
            }


            c.moveToNext();
        }
        c.close();
    }

    public ArrayList<String[]> getLastSmss() {
        ArrayList<String[]> res = new ArrayList<>();

        Cursor c = db.rawQuery("" +
                "SELECT s2.* FROM (\n" +
                "   SELECT sms_number, MAX(sms_timems) as max_timems FROM sms GROUP BY sms_number\n" +
                ") s1 INNER JOIN sms s2 ON s1.sms_number=s2.sms_number AND \n" +
                "s1.max_timems=s2.sms_timems", null);

        if (!c.moveToFirst()) {
            c.close();
            return res;
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

        return res;
    }

    public void close() {
        if (db == null) {
            Log.e(TAG, "db is not opened");
            return;
        }

        db.close();
    }
}
