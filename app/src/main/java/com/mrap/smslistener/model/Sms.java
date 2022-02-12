package com.mrap.smslistener.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.provider.Telephony;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class Sms {
    private static final String TAG = "model.Sms";

    public int id;
    public long date;
    public String addr;
    public String body;
    public int type = Telephony.Sms.MESSAGE_TYPE_INBOX;
    public boolean read = false;

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
            oldVerDb = SmsSqliteHandler.openDb_0(context);
            smss = SmsSqliteHandler.getSmss_0(oldVerDb);
        } else {
            return;
        }
        oldVerDb.close();

        SQLiteDatabase newDb = SmsSqliteHandler_v1.openDb(context);
        SmsSqliteHandler_v1.insertSmss(newDb, smss);
        newDb.close();
    }

    public static String createLimit(int offset, int limit, boolean excludeLimit) {
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

    public static ArrayList<Sms> getLastSmssFromBoth(
            SQLiteDatabase smsDb, Context context, int offset, int limit) throws Exception {
        ArrayList<Sms> allSmss = SmsSqliteHandler_v1.getLastSmss(smsDb, offset, limit);
        allSmss.addAll(SmsContentProviderHandler.getLastSmssFromContentResolver(context, offset, limit));
        sortAndTrim(allSmss, limit);
        return allSmss;
    }

    public static ArrayList<Sms> getSmssFromBoth(
            SQLiteDatabase smsDb, Context context, String address, int offset, int limit) {
        ArrayList<Sms> allSmss = SmsSqliteHandler_v1.getSmss(smsDb, address, offset, limit);
        allSmss.addAll(SmsContentProviderHandler.getSmssFromContentResolver(context, address, offset, limit));
        sortAndTrim(allSmss, limit);
        return allSmss;
    }

    public static void sortAndTrim(ArrayList<Sms> smss, int limit) {
        Collections.sort(smss, (o1, o2) -> Long.compare(o2.date, o1.date));

        if (smss.size() > limit) {
            for (int i = smss.size() - 1; i >= limit; i--) {
                smss.remove(i);
            }
        }
    }
}
