package com.mrap.smslistener.model;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.Telephony;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;

public class SmsContentResolverModel {

    private static final String TAG = "SmsCntRslvrModel";

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

        Log.d(TAG, "loaded " + res.size() + " smss in " +
                (System.currentTimeMillis() - startMs) + " ms");

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
        Log.d(TAG, "getSmss");

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

            if (oldestFilterDate-date > 30000) {
                break;
            }

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
