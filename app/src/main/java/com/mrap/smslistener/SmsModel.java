package com.mrap.smslistener;

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
import java.util.HashSet;

public abstract class SmsModel {

    private static final String TAG = "SmsModel";

    public static class Sms {
        long date;
        String addr;
        String body;
        int type = Telephony.Sms.MESSAGE_TYPE_INBOX;
        boolean read = false;
    }

    private final static String onCreateSql_0 = "" +
            "CREATE TABLE IF NOT EXISTS sms (\n" +
            "   sms_id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
            "   sms_number TEXT,\n" +
            "   sms_message TEXT,\n" +
            "   sms_timems INTEGER\n" +
            ")";

    private final static String onCreateSql_1 = "" +
            "CREATE TABLE IF NOT EXISTS sms (\n" +
            "   sms_id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
            "   sms_addr TEXT,\n" +
            "   sms_body TEXT,\n" +
            "   sms_timems INTEGER,\n" +
            "   sms_read INTEGER\n" +
            ");" +
            "CREATE INDEX IF NOT EXISTS idx_sms_addr ON sms(sms_addr);";

    private final static int CURRENT_DB_VER = 1;

    public static int checkDbLastUsedVersion(Context context) {
        Log.d(TAG, "checkDbLastUsedVersion");
        File dir = context.getExternalFilesDir(null);
        File[] children = dir.listFiles();
        if (children == null) {
            return -1;
        }
        int maxVer = -1;
        for (File f : children) {
            String name = f.getName();
            Log.d(TAG, "name " + name);
            int nameLen = name.length();
            if (name.indexOf("sms") == 0 && name.indexOf(".db") == nameLen - 3) {
                String verAndExt = name.substring(3);
                String[] parts = verAndExt.split("\\.");
                Log.d(TAG, "parts len " + parts.length);
                for (int i = 0; i < parts.length; i++) {
                    Log.d(TAG, "part " + i + " " + parts[i]);
                }
                if (parts.length == 0) {
                    continue;
                }
                if (!parts[parts.length - 1].equals("db")) {
                    continue;
                }
                int ver;
                if (parts.length == 1) {
                    ver = 0;
                    if (ver > maxVer) {
                        maxVer = ver;
                    }
                    continue;
                }
                String verStr = parts[parts.length - 2];
                if (verStr.isEmpty()) {
                    ver = 0;
                    if (ver > maxVer) {
                        maxVer = ver;
                    }
                    continue;
                }
                try {
                    ver = Integer.parseInt(verStr.substring(1));
                    if (ver > maxVer) {
                        maxVer = ver;
                    }
                } catch (NumberFormatException ignored) { }
            }
        }
        return maxVer;
    }

    public static void migrateToLatestVersion(Context context) {
        Log.d(TAG, "migrateToLatestVersion");
        int lastUsedVer = checkDbLastUsedVersion(context);
        Log.d(TAG, "last used ver is " + lastUsedVer);
        if (lastUsedVer == CURRENT_DB_VER) {
            Log.d(TAG, "already new version");
            return;
        }
        if (lastUsedVer == -1) {
            Log.d(TAG, "no db to migrate");
            return;
        }

        SQLiteDatabase oldVerDb;
        ArrayList<Sms> smss;

        if (lastUsedVer == 0) {
            oldVerDb = openDb_0(context);
            smss = getSmss_0(oldVerDb);
        } else {
            return;
        }
        oldVerDb.close();

        SQLiteDatabase newDb = openDb(context);
        insertSmss(newDb, smss);
        newDb.close();
    }

    public static SQLiteDatabase openDb(Context context) {
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

    private static String createLimit(int offset, int limit, boolean excludeLimit) {
        String limitStr = null;
        if (limit > 0) {
            if (offset > 0) {
                limitStr = "" + limit;
            } else {
                limitStr = offset + "," + limit;
            }
            if (excludeLimit) {
            } else {
                limitStr = "LIMIT " + limitStr;
            }
        }
        return limitStr;
    }

    public static ArrayList<Sms> getSmss(SQLiteDatabase smsDb, int offset, int limit) {
        ArrayList<Sms> res = new ArrayList<>();

        Cursor c = smsDb.query("sms", new String[] {"sms_addr", "sms_body", "sms_timems",
                        "sms_read"}, null, null, null, null,
                "sms_timems DESC", createLimit(offset, limit, true));

        if (!c.moveToFirst()) {
            c.close();
            return res;
        }

        int addrIdx = c.getColumnIndex("sms_addr");
        int bodyIdx = c.getColumnIndex("sms_body");
        int timeIdx = c.getColumnIndex("sms_timems");
        int readIdx = c.getColumnIndex("sms_read");
        do {
            Sms row = new Sms() {{
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

    public static ArrayList<Sms> getSmss(SQLiteDatabase smsDb, String addr, int offset, int limit) {
        ArrayList<Sms> res = new ArrayList<>();

        Cursor c = smsDb.query("sms", new String[] {"sms_addr", "sms_body", "sms_timems",
                        "sms_read"}, "sms_addr='" + addr + "'", null,
                null, null, "sms_timems DESC", createLimit(offset, limit,
                        true));
        if (!c.moveToFirst()) {
            c.close();
            return res;
        }

        int addrIdx = c.getColumnIndex("sms_addr");
        int bodyIdx = c.getColumnIndex("sms_body");
        int timeIdx = c.getColumnIndex("sms_timems");
        int readIdx = c.getColumnIndex("sms_read");
        do {
            Sms row = new Sms() {{
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

    public static ArrayList<Sms> getLastSmss(SQLiteDatabase smsDb, int offset, int limit) {
        ArrayList<Sms> res = new ArrayList<>();

//        Cursor c = smsDb.rawQuery("" +
//                "SELECT s2.* FROM (\n" +
//                "   SELECT sms_addr, MAX(sms_timems) as max_timems FROM sms GROUP BY sms_addr\n" +
//                ") s1 INNER JOIN sms s2 ON s1.sms_addr=s2.sms_addr AND \n" +
//                "s1.max_timems=s2.sms_timems ORDER BY s2.sms_timems DESC", null);

        Cursor c = smsDb.query("sms", new String[] {"sms_addr", "sms_body", "sms_timems",
                        "sms_read"}, null, null, null, null,
                "sms_timems DESC");

        if (!c.moveToFirst()) {
            c.close();
            return res;
        }

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

    public static ArrayList<Sms> getLastSmssFromContentResolver(
            Context context, int offset, int limit) throws Exception {
        ArrayList<Sms> res = new ArrayList<>();

        long startMs = System.currentTimeMillis();

        ContentResolver cr = context.getContentResolver();

        // 346ms 671row
        Cursor c = cr.query(Telephony.Sms.Inbox.CONTENT_URI, null, null,
                null, Telephony.Sms.DATE + " DESC");

        // fail
//        Cursor c = cr.query(Telephony.Sms.Conversations.CONTENT_URI, null, null,
//                null, Telephony.Sms.DATE + " DESC");

        if (c == null) {
            Log.d(TAG, "load fail");
            return res;
        }

        if (!c.moveToFirst()) {
            Log.d(TAG, "load fail");
            c.close();
            return res;
        }

        int idxTime = c.getColumnIndexOrThrow(Telephony.Sms.DATE);
        int idxAddr = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS);
        int idxBody = c.getColumnIndexOrThrow(Telephony.Sms.BODY);
//        int idxType = c.getColumnIndexOrThrow(Telephony.Sms.TYPE);
        int idxRead = c.getColumnIndexOrThrow(Telephony.Sms.READ);

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

        Log.d(TAG, "loaded in " + (System.currentTimeMillis() - startMs) + " ms");

        return res;
    }

    public static ArrayList<Sms> getSmssFromContentResolver(
            Context context, String addr, int offset, int limit) {
        return getSmssFromContentResolver(
                context,
                Telephony.Sms.ADDRESS + "='" + addr + "'",
                null,
                offset, limit);
    }

    private static ArrayList<Sms> getSmssFromContentResolver(
            Context context, String selection, String[] selectionArgs, int offset, int limit) {
        ArrayList<Sms> res = new ArrayList<>();

        ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(Telephony.Sms.Inbox.CONTENT_URI, null, selection,
                null, Telephony.Sms.DATE + " DESC");

        if (c == null) {
            return res;
        }

        if (!c.moveToFirst()) {
            c.close();
            return res;
        }

        int idxDate = c.getColumnIndexOrThrow(Telephony.Sms.DATE);
        int idxAddr = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS);
        int idxBody = c.getColumnIndexOrThrow(Telephony.Sms.BODY);
        int idxType = c.getColumnIndexOrThrow(Telephony.Sms.TYPE);

        if (offset > 0) {
            c.move(offset);
        }

        int i = 0;
        do {
            Sms sms = new Sms() {{
                date = Long.parseLong(c.getString(idxDate));
                addr = c.getString(idxAddr);
                body = c.getString(idxBody);
                type = Integer.parseInt(c.getString(idxType));
            }};
            res.add(sms);

            i++;

            if (limit > 0) {
                if (i > limit) {
                    break;
                }
            }
        } while (c.moveToNext());
        c.close();

        return res;
    }

    public static ArrayList<Sms> getSmssFromContentResolver(
            Context context, ArrayList<Sms> filterSmss) {
        ArrayList<Sms> res = new ArrayList<>();
        if (filterSmss.size() == 0) {
            return res;
        }

        ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(Telephony.Sms.Inbox.CONTENT_URI, null, null,
                null, Telephony.Sms.DATE + " DESC");
        if (c == null) {
            return res;
        }
        if (!c.moveToFirst()) {
            c.close();
            return res;
        }

        int idxDate = c.getColumnIndexOrThrow(Telephony.Sms.DATE);
        int idxAddr = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS);
        int idxBody = c.getColumnIndexOrThrow(Telephony.Sms.BODY);

        long oldestFilterDate = filterSmss.get(filterSmss.size() - 1).date;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm");
        Log.d(TAG, "fetching content resolver smss (" + c.getCount() + ")");
        do {
            if (res.size() == filterSmss.size()) {
                break;
            }

            long date = Long.parseLong(c.getString(idxDate));
            String addr = c.getString(idxAddr);
            String body = c.getString(idxBody);

//            if (oldestFilterDate-date > 30000) {
//                break;
//            }

//            Log.d(TAG, addr + ":" + date + ": ==" + body + "==");

            for (int i = 0; i < filterSmss.size(); i++) {
                Sms dbSms = filterSmss.get(i);
                if (Math.abs(dbSms.date-date) < 30000 && dbSms.addr.equals(addr) &&
                        dbSms.body.equals(body)) {
                    res.add(dbSms);
                }
            }
        } while (c.moveToNext());
        c.close();
        Log.d(TAG, "done fetching content resolver smss");

        return res;
    }
}
