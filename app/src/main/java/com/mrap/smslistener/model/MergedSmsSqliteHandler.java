package com.mrap.smslistener.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.provider.Telephony;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

public class MergedSmsSqliteHandler extends SqliteHandler {
    private static final String TAG = "MergdSmsSqliteHndlr";

    private static final String[] createSqls = {"" +
            "CREATE TABLE IF NOT EXISTS sms (\n" +
            "   sms_id INTEGER,\n" +
            "   sms_source INTEGER," +
            "   sms_addr TEXT,\n" +
            "   sms_body TEXT,\n" +
            "   sms_type INTEGER,\n" +
            "   sms_timems INTEGER,\n" +
            "   sms_read INTEGER\n" +
            ")", "" +
            "CREATE INDEX IF NOT EXISTS idx_sms_id ON sms(sms_id)",
            "CREATE INDEX IF NOT EXISTS idx_sms_source ON sms(sms_source)",
            "CREATE INDEX IF NOT EXISTS idx_sms_addr ON sms(sms_addr)"
    };

    private static final Object mergedSmsLock = new Object();
    private static final Object tmpMergedSmsLock = new Object();

    private static final String mergedSmsRelPath = "mergedsms.db";
    private static final String tmpMergedSmsRelPath = "mergedsms.tmp.db";
    private static boolean syncRunning = false;

    public static SQLiteDatabase openDb(Context context) {
        return openDb(context, context.getExternalFilesDir(null) + "/" +
                mergedSmsRelPath, createSqls);
    }

    public static int syncContentProvider(Context context) {
        if (syncRunning) {
            return -1;
        }

        int res = 0;
        syncRunning = true;

        try {
            syncContentProviderInternal(context);
        } catch (Exception e) {
            e.printStackTrace();
            res = -2;
        }

        syncRunning = false;
        return res;
    }

    public static boolean isSyncRunning() {
        return syncRunning;
    }

    private static void syncContentProviderInternal(Context context) throws Exception {
        Log.d(TAG, "syncing to content provider");
        synchronized (tmpMergedSmsLock) {
            String tmpPath = context.getExternalFilesDir(null) + "/" + tmpMergedSmsRelPath;
            File tmpFile = new File(tmpPath);
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
            File tmpFile2 = new File(tmpFile + "-shm");
            if (tmpFile2.exists()) {
                tmpFile2.delete();
            }
            tmpFile2 = new File(tmpFile + "-wal");
            if (tmpFile2.exists()) {
                tmpFile2.delete();
            }

            SQLiteDatabase tmpDb = openDb(context, tmpPath, createSqls);

            ArrayList<Sms> oldSqliteSmss = new ArrayList<>();
            ArrayList<Sms> smss;

            File mergedSmsFile = new File(context.getExternalFilesDir(null) +
                    "/" + mergedSmsRelPath);

            synchronized (mergedSmsLock) {
                if (mergedSmsFile.exists()) {
                    SQLiteDatabase mergedSmsDb = openDb(context);

                    Log.d(TAG, "getting smss from old db which source is not content " +
                            "provider source");
                    smss = getSmss(mergedSmsDb, "sms_source=" + Sms.SOURCE_SQLITE,
                            "sms_timems DESC", 0, 0, null);
                    oldSqliteSmss.addAll(smss);
                    Log.d(TAG, "got " + oldSqliteSmss.size() + " smss");

                    mergedSmsDb.close();
                }
            }

            long oldestOldSmsDate = oldSqliteSmss.size() > 0 ?
                    oldSqliteSmss.get(oldSqliteSmss.size() - 1).date : -1;

            tmpDb.beginTransaction();

            int[] removedOldSqliteSmss = new int[] {0};

            Log.d(TAG, "begin copying from content provider");
            smss = SmsContentProviderHandler.getSmss(context, null,
                    Telephony.Sms.DATE + " DESC", 0, 0, sms -> {
                        if (oldestOldSmsDate > 0 && oldestOldSmsDate - sms.date < 30000) {
                            // remove already updated in content provider
                            for (int i = oldSqliteSmss.size() - 1; i >= 0; i--) {
                                Sms dbSms = oldSqliteSmss.get(i);
                                if (Math.abs(dbSms.date - sms.date) < 30000 &&
                                        dbSms.addr.equals(sms.addr) &&
                                        dbSms.body.equals(sms.body)) {
                                    oldSqliteSmss.remove(i);
                                    removedOldSqliteSmss[0]++;
                                }
                            }
                        }

                        String insertSql = "INSERT INTO sms (sms_id, sms_source, sms_addr, sms_body, " +
                                "sms_type, sms_timems, sms_read) VALUES (?, ?, ?, ?, ?, ?, ?)";
                        SQLiteStatement statement = tmpDb.compileStatement(insertSql);
                        statement.clearBindings();
                        statement.bindLong(1, sms.id);
                        statement.bindLong(2, Sms.SOURCE_CONTENTPROVIDER);
                        statement.bindString(3, sms.addr);
                        statement.bindString(4, sms.body);
                        statement.bindLong(5, sms.type);
                        statement.bindLong(6, sms.date);
                        statement.bindLong(7, sms.read ? 1 : 0);
                        statement.executeInsert();
                    });
            Log.d(TAG, "copied " + smss.size() + " smss. removed old sqlite smss: " +
                    removedOldSqliteSmss[0]);

            long newestCntPrvderSmsDate = smss.size() > 0 ? smss.get(0).date :
                    -1;

            Log.d(TAG, "inserting old sqlite smss, if there is still remaining: " +
                    oldSqliteSmss.size());
            int count = 0;
            for (int i = 0; i < oldSqliteSmss.size(); i++, count++) {
                Sms sms = oldSqliteSmss.get(i);
                if (sms.date < newestCntPrvderSmsDate - 30000) {
                    break;
                }
                String insertSql = "INSERT INTO sms (sms_id, sms_source, sms_addr, " +
                        "sms_body, sms_type, sms_timems, sms_read) VALUES " +
                        "(?, ?, ?, ?, ?, ?, ?)";
                SQLiteStatement statement = tmpDb.compileStatement(insertSql);
                statement.clearBindings();
                statement.bindLong(1, sms.id);
                statement.bindLong(2, Sms.SOURCE_SQLITE);
                statement.bindString(3, sms.addr);
                statement.bindString(4, sms.body);
                statement.bindLong(5, sms.type);
                statement.bindLong(6, sms.date);
                statement.bindLong(7, sms.read ? 1 : 0);
                statement.executeInsert();
            }
            Log.d(TAG, "inserted " + count + " old sqlite smss, " +
                    (oldSqliteSmss.size() - count) + " are skipped because older than latest " +
                    "content provider sms");

            tmpDb.setTransactionSuccessful();
            tmpDb.endTransaction();

            tmpDb.close();

            synchronized (mergedSmsLock) {
                mergedSmsFile.delete();
                Log.d(TAG, "renaming tmp to db");
                tmpFile.renameTo(mergedSmsFile);
            }
        }
    }

    public static void insertSms(
            SQLiteDatabase smsDb, String addr, String body, long timestamp, boolean read) {

        ArrayList<Sms> smss = getSmss(smsDb, null, "sms_id DESC", 0,
                1, null);
        long id;
        if (smss.size() > 0) {
            id = smss.get(0).id + 1;
        } else {
            id = 1;
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put("sms_id", id);
        contentValues.put("sms_addr", addr);
        contentValues.put("sms_body", body);
        contentValues.put("sms_timems", timestamp);
        contentValues.put("sms_read", read ? 1 : 0);
        contentValues.put("sms_type", Telephony.Sms.MESSAGE_TYPE_INBOX);
        contentValues.put("sms_source", Sms.SOURCE_SQLITE);
        smsDb.insert("sms", null, contentValues);
    }

    public static ArrayList<Sms> getSmss(
            SQLiteDatabase mergedSmsDb, String selection, String orderBy, int offset, int limit,
            Callback<Sms> onEach) {
        synchronized (mergedSmsLock) {
            ArrayList<Sms> res = new ArrayList<>();
            Cursor c = mergedSmsDb.query("sms", new String[]{"*"}, selection,
                    null, null, null, orderBy,
                    Sms.createLimit(offset, limit, true));
            if (!c.moveToFirst()) {
                c.close();
                return res;
            }

            int idxId = c.getColumnIndex("sms_id");
            int idxTime = c.getColumnIndex("sms_timems");
            int idxAddr = c.getColumnIndex("sms_addr");
            int idxBody = c.getColumnIndex("sms_body");
            int idxType = c.getColumnIndex("sms_type");
            int idxRead = c.getColumnIndex("sms_read");
            int idxSource = c.getColumnIndex("sms_source");

            int inCount = 0, outCount = 0;

            do {
                Sms sms = new Sms() {{
                    id = c.getLong(idxId);
                    source = c.getInt(idxSource);
                    addr = c.getString(idxAddr);
                    body = c.getString(idxBody);
                    type = c.getInt(idxType);
                    date = c.getLong(idxTime);
                    read = c.getInt(idxRead) != 0;
                }};
                if (sms.type == Telephony.Sms.MESSAGE_TYPE_INBOX) {
                    inCount++;
                } else if (sms.type == Telephony.Sms.MESSAGE_TYPE_SENT) {
                    outCount++;
                }
                if (onEach != null) {
                    onEach.onCallback(sms);
                }
                res.add(sms);
            } while (c.moveToNext());
            c.close();

            Log.d(TAG, "in " + inCount + " out " + outCount);

            return res;
        }
    }

    public static ArrayList<Sms> getLastSmss(
            SQLiteDatabase mergedSmsDb, int offset, int limit, Callback<Sms> onEach) {
        ArrayList<Sms> res = new ArrayList<>();
        Cursor c = mergedSmsDb.query("sms", new String[] {"*"}, null, null, null,
                null, "sms_timems DESC");

        if (!c.moveToFirst()) {
            c.close();
            return res;
        }

        int idxId = c.getColumnIndex("sms_id");
        int idxAddr = c.getColumnIndex("sms_addr");
        int idxBody = c.getColumnIndex("sms_body");
        int idxTime = c.getColumnIndex("sms_timems");
        int idxRead = c.getColumnIndex("sms_read");
        int idxType = c.getColumnIndex("sms_type");
        int idxSource = c.getColumnIndex("sms_source");

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
                type = c.getInt(idxType);
                read = c.getInt(idxRead) != 0;
                source = c.getInt(idxSource);
            }};
            res.add(row);
            addrSet.add(rowAddr);
            count++;

            if (limit > 0 && count >= limit) {
                break;
            }

        } while (c.moveToNext());

        c.close();

        return res;
    }

//    public static void migrateToMergedSms(Context context) {
//        synchronized (mergedSmsLock) {
//            SQLiteDatabase smsDb = SmsSqliteHandler_v1.openDb(context);
//            SQLiteDatabase mergedSmsDb = openDb(context);
//            mergedSmsDb.beginTransaction();
//            SmsSqliteHandler_v1.getSmss(smsDb, null, null, 0, 0,
//                    sms -> {
//                        String insertSql = "INSERT INTO sms (sms_source, sms_addr, " +
//                                "sms_body, sms_type, sms_timems, sms_read) VALUES " +
//                                "(?, ?, ?, ?, ?, ?)";
//                        SQLiteStatement statement = mergedSmsDb.compileStatement(insertSql);
//                        statement.clearBindings();
//                        statement.bindLong(1, Sms.SOURCE_SQLITE);
//                        statement.bindString(2, sms.addr);
//                        statement.bindString(3, sms.body);
//                        statement.bindLong(4, sms.type);
//                        statement.bindLong(5, sms.date);
//                        statement.bindLong(6, sms.read ? 1 : 0);
//                        statement.executeInsert();
//                    });
//            mergedSmsDb.setTransactionSuccessful();
//            mergedSmsDb.endTransaction();
//            smsDb.close();
//            mergedSmsDb.close();
//        }
//    }

    public static boolean isDbExists(Context context) {
        File file = new File(context.getExternalFilesDir(null) + "/" +
                mergedSmsRelPath);
        return file.exists();
    }
}
