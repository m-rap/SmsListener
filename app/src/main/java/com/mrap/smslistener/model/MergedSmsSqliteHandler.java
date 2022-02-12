package com.mrap.smslistener.model;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.provider.Telephony;

import java.io.File;

public class MergedSmsSqliteHandler extends SqliteHandler {
    private static final String TAG = "MergdSmsSqliteHndlr";
    public static final int SOURCE_CONTENTPROVIDER = 0;
    public static final int SOURCE_SQLITE = 1;

    private static String createSql = "" +
            "CREATE TABLE IF NOT EXISTS sms (\n" +
            "   sms_id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
            "   sms_source INTEGER," +
            "   sms_addr TEXT,\n" +
            "   sms_body TEXT,\n" +
            "   sms_timems INTEGER,\n" +
            "   sms_read INTEGER\n" +
            ");" +
            "CREATE INDEX IF NOT EXISTS idx_sms_addr ON sms(sms_addr);";

    public static SQLiteDatabase openDb(Context context) {
        return openDb(context, context.getExternalFilesDir(null) + "/mergedsms.db",
                createSql);
    }

    public static void syncContentResolver(Context context) {
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

        ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(Telephony.Sms.Inbox.CONTENT_URI, null, null,
                null, null);

        if (c != null) {
            if (c.moveToFirst()) {
                int idxTime = c.getColumnIndexOrThrow(Telephony.Sms.DATE);
                int idxAddr = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS);
                int idxBody = c.getColumnIndexOrThrow(Telephony.Sms.BODY);
                //        int idxType = c.getColumnIndexOrThrow(Telephony.Sms.TYPE);
                int idxRead = c.getColumnIndexOrThrow(Telephony.Sms.READ);

                tmpDb.beginTransaction();
                do {
                    String insertSql = "INSERT INTO sms (sms_source, sms_addr, sms_body, sms_timems," +
                            "sms_read) VALUES (?, ?, ?, ?, ?)";
                    SQLiteStatement statement = tmpDb.compileStatement(insertSql);
                    statement.clearBindings();
                    statement.bindLong(1, SOURCE_CONTENTPROVIDER);
                    statement.bindString(2, c.getString(idxAddr));
                    statement.bindString(3, c.getString(idxBody));
                    statement.bindLong(4, c.getLong(idxTime));
                    statement.bindLong(5, c.getInt(idxRead));
                    statement.executeInsert();
                } while (c.moveToNext());
                tmpDb.setTransactionSuccessful();
                tmpDb.endTransaction();
            }
            c.close();
        }

        tmpDb.close();

        tmpFile.renameTo(new File(context.getExternalFilesDir(null) +
                "/mergedsms.db"));
    }
}
