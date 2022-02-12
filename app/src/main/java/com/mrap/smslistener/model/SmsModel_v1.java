package com.mrap.smslistener.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.Telephony;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;

public class SmsModel_v1 {
    private static final String TAG = "SmsModel_v1";

    private final static String onCreateSql_1 = "" +
            "CREATE TABLE IF NOT EXISTS sms (\n" +
            "   sms_id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
            "   sms_addr TEXT,\n" +
            "   sms_body TEXT,\n" +
            "   sms_timems INTEGER,\n" +
            "   sms_read INTEGER\n" +
            ");" +
            "CREATE INDEX IF NOT EXISTS idx_sms_addr ON sms(sms_addr);";

    public static SQLiteDatabase openDb(Context context) {
        Log.d(TAG, "opening db");
        SQLiteOpenHelper sqLiteOpenHelper = new SQLiteOpenHelper(context,
                context.getExternalFilesDir(null) + "/sms.v1.db", null, 1) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL(onCreateSql_1);
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            }
        };
        return sqLiteOpenHelper.getWritableDatabase();
    }

    public static void insertSms(
            SQLiteDatabase smsDb, String addr, String body, long timestamp, boolean read) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("sms_addr", addr);
        contentValues.put("sms_body", body);
        contentValues.put("sms_timems", timestamp);
        contentValues.put("sms_read", read ? 1 : 0);
        smsDb.insert("sms", null, contentValues);
    }

    public static void insertSmss(SQLiteDatabase smsDb, ArrayList<Sms> smss) {
        smsDb.beginTransaction();
        for (Sms sms : smss) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("sms_addr", sms.addr);
            contentValues.put("sms_body", sms.body);
            contentValues.put("sms_timems", sms.date);
            contentValues.put("sms_read", sms.read);
            smsDb.insert("sms", null, contentValues);
        }
        smsDb.setTransactionSuccessful();
        smsDb.endTransaction();
    }

    public static ArrayList<Sms> getSmss(SQLiteDatabase smsDb, int offset, int limit) {
        ArrayList<Sms> res = new ArrayList<>();

        Cursor c = smsDb.query("sms", new String[] {"sms_id", "sms_addr", "sms_body",
                        "sms_timems", "sms_read"}, null, null, null,
                null, "sms_timems DESC", Sms.createLimit(offset, limit, true));

        if (!c.moveToFirst()) {
            c.close();
            return res;
        }

        int idIdx = c.getColumnIndex("sms_id");
        int addrIdx = c.getColumnIndex("sms_addr");
        int bodyIdx = c.getColumnIndex("sms_body");
        int timeIdx = c.getColumnIndex("sms_timems");
        int readIdx = c.getColumnIndex("sms_read");
        do {
            Sms row = new Sms() {{
                id = c.getInt(idIdx);
                date = c.getLong(timeIdx);
                addr = c.getString(addrIdx);
                body = c.getString(bodyIdx);
                type = Telephony.Sms.MESSAGE_TYPE_INBOX;
                read = c.getInt(readIdx) != 0;
            }};
            res.add(row);
        } while (c.moveToNext());

        c.close();
        return res;
    }

    public static ArrayList<Sms> getSmss(SQLiteDatabase smsDb, String addr, int offset, int limit) {
        Log.d(TAG, "getSmss");

        ArrayList<Sms> res = new ArrayList<>();

        Cursor c = smsDb.query("sms", new String[] {"sms_id", "sms_addr", "sms_body", "sms_timems",
                        "sms_read"}, "sms_addr='" + addr + "'", null,
                null, null, "sms_timems DESC", Sms.createLimit(offset, limit,
                        true));
        if (!c.moveToFirst()) {
            c.close();
            return res;
        }

        int idIdx = c.getColumnIndex("sms_id");
        int addrIdx = c.getColumnIndex("sms_addr");
        int bodyIdx = c.getColumnIndex("sms_body");
        int timeIdx = c.getColumnIndex("sms_timems");
        int readIdx = c.getColumnIndex("sms_read");
        do {
            Sms row = new Sms() {{
                id = c.getInt(idIdx);
                addr = c.getString(addrIdx);
                body = c.getString(bodyIdx);
                date = c.getLong(timeIdx);
                type = Telephony.Sms.MESSAGE_TYPE_INBOX;
                read = c.getInt(readIdx) != 0;
            }};
            res.add(row);
        } while (c.moveToNext());

        c.close();
        return res;
    }

    public static ArrayList<Sms> getLastSmss(SQLiteDatabase smsDb, int offset, int limit) {
        ArrayList<Sms> res = new ArrayList<>();

//        Cursor c = smsDb.rawQuery("" +
//                "SELECT s2.* FROM (\n" +
//                "   SELECT sms_addr, MAX(sms_timems) as max_timems FROM sms GROUP BY sms_addr\n" +
//                ") s1 INNER JOIN sms s2 ON s1.sms_addr=s2.sms_addr AND \n" +
//                "s1.max_timems=s2.sms_timems ORDER BY s2.sms_timems DESC", null);

        Cursor c = smsDb.query("sms", new String[] {"sms_id", "sms_addr", "sms_body", "sms_timems",
                        "sms_read"}, null, null, null, null,
                "sms_timems DESC");

        if (!c.moveToFirst()) {
            c.close();
            return res;
        }

        int idxId = c.getColumnIndex("sms_id");
        int idxAddr = c.getColumnIndex("sms_addr");
        int idxBody = c.getColumnIndex("sms_body");
        int idxTime = c.getColumnIndex("sms_timems");
        int idxRead = c.getColumnIndex("sms_read");

        int count = 0;
        int offsetCount = 0;

        HashSet<String> addrSet = new HashSet<>();

        do {
            String rowAddr = c.getString(idxAddr);
            if (addrSet.contains(rowAddr)) {
                continue;
            }

            if (offset > 0 && offsetCount < offset) {
                offsetCount++;
                continue;
            }

            Sms row = new Sms() {{
                id = c.getInt(idxId);
                addr = rowAddr;
                body = c.getString(idxBody);
                date = c.getLong(idxTime);
                type = Telephony.Sms.MESSAGE_TYPE_INBOX;
                read = c.getInt(idxRead) != 0;
            }};
            res.add(row);
            addrSet.add(rowAddr);
            count++;

            if (limit > 0 && count > limit) {
                break;
            }

        } while (c.moveToNext());

        c.close();

        return res;
    }

    public static void cleanupDbAlreadyInContentResolver(SQLiteDatabase smsDb, Context context) {
        ArrayList<Sms> dbSmss = SmsModel_v1.getSmss(smsDb, -1, -1);
        ArrayList<Sms> matchedSmss = SmsContentResolverModel.getSmssFromContentResolver(context, dbSmss);

        if (matchedSmss.size() == 0) {
            return;
        }

        String ids = "";
        for (int i = 0; i < matchedSmss.size(); i++) {
            if (i < matchedSmss.size() - 1) {
                ids += matchedSmss.get(i).id + ",";
            } else {
                ids += matchedSmss.get(i).id;
            }
        }

        Log.d(TAG, "deleting sms ids " + ids);
        smsDb.delete("sms", "sms_id IN (" + ids + ")", null);
    }
}
