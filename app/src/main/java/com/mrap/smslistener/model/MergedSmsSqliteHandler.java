package com.mrap.smslistener.model;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.provider.Telephony;

import java.io.File;
import java.util.ArrayList;

public class MergedSmsSqliteHandler extends SqliteHandler {
    private static final String TAG = "MergdSmsSqliteHndlr";

    private static final String createSql = "" +
            "CREATE TABLE IF NOT EXISTS sms (\n" +
            "   sms_id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
            "   sms_source INTEGER," +
            "   sms_addr TEXT,\n" +
            "   sms_body TEXT,\n" +
            "   sms_type INTEGER,\n" +
            "   sms_timems INTEGER,\n" +
            "   sms_read INTEGER\n" +
            ");" +
            "CREATE INDEX IF NOT EXISTS idx_sms_addr ON sms(sms_addr);";

    private static final Object mergedSmsLock = new Object();
    private static final Object tmpMergedSmsLock = new Object();

    public static SQLiteDatabase openDb(Context context) {
        return openDb(context, context.getExternalFilesDir(null) + "/mergedsms.db",
                createSql);
    }

    public static void syncContentResolver(Context context) {
        synchronized (tmpMergedSmsLock) {
            String tmpPath = context.getExternalFilesDir(null) + "/mergedsms.tmp.db";
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

            SQLiteDatabase tmpDb = openDb(context, tmpPath, createSql);

            tmpDb.beginTransaction();
            SmsContentProviderHandler.getSmss(context, null, null, 0, 0,
                    sms -> {
                        String insertSql = "INSERT INTO sms (sms_source, sms_addr, sms_body, " +
                                "sms_type, sms_timems, sms_read) VALUES (?, ?, ?, ?, ?, ?)";
                        SQLiteStatement statement = tmpDb.compileStatement(insertSql);
                        statement.clearBindings();
                        statement.bindLong(1, Sms.SOURCE_CONTENTPROVIDER);
                        statement.bindString(2, sms.addr);
                        statement.bindString(3, sms.body);
                        statement.bindLong(4, sms.type);
                        statement.bindLong(5, sms.date);
                        statement.bindLong(6, sms.read ? 1 : 0);
                        statement.executeInsert();
                    });
            tmpDb.setTransactionSuccessful();
            tmpDb.endTransaction();

            synchronized (mergedSmsLock) {
                File mergedSmsFile = new File(context.getExternalFilesDir(null) +
                        "/mergedsms.db");
                if (mergedSmsFile.exists()) {
                    SQLiteDatabase mergedSmsDb = openDb(context);
                    tmpDb.beginTransaction();
                    getSmss(mergedSmsDb, "sms_source=" + Sms.SOURCE_SQLITE, null,
                            0, 0, sms -> {
                                String insertSql = "INSERT INTO sms (sms_source, sms_addr, " +
                                        "sms_body, sms_type, sms_timems, sms_read) VALUES " +
                                        "(?, ?, ?, ?, ?, ?)";
                                SQLiteStatement statement = tmpDb.compileStatement(insertSql);
                                statement.clearBindings();
                                statement.bindLong(1, Sms.SOURCE_SQLITE);
                                statement.bindString(2, sms.addr);
                                statement.bindString(3, sms.body);
                                statement.bindLong(4, sms.type);
                                statement.bindLong(5, sms.date);
                                statement.bindLong(6, sms.read ? 1 : 0);
                                statement.executeInsert();
                            });
                    tmpDb.setTransactionSuccessful();
                    tmpDb.endTransaction();
                    mergedSmsDb.close();
                    mergedSmsFile.delete();
                }
                tmpDb.close();

                tmpFile.renameTo(mergedSmsFile);
            }
        }
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
            int idxTime = c.getColumnIndex("sms_timems");
            int idxAddr = c.getColumnIndex("sms_addr");
            int idxBody = c.getColumnIndex("sms_body");
            int idxType = c.getColumnIndex("sms_type");
            int idxRead = c.getColumnIndex("sms_read");
            do {
                Sms sms = new Sms() {{
                    source = SOURCE_SQLITE;
                    addr = c.getString(idxAddr);
                    body = c.getString(idxBody);
                    type = (int) c.getLong(idxType);
                    date = c.getLong(idxTime);
                    read = c.getLong(idxRead) != 0;
                }};
                if (onEach != null) {
                    onEach.onCallback(sms);
                }
                res.add(sms);
            } while (c.moveToNext());
            c.close();
            return res;
        }
    }
}
