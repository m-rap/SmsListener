package com.mrap.smslistener.model;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.Telephony;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

public class SmsContentProviderHandler {

    private static final String TAG = "SmsCntPrvdrHndlr";

    public static ArrayList<Sms> getSmss(
            Context context, String selection, String orderBy, int offset, int limit,
            Callback<Sms> onEach) {
        ArrayList<Sms> res = new ArrayList<>();

//        String limitStr = Sms.createLimit(offset, limit, false);
//        if (orderBy == null) {
//            if (limitStr != null) {
//                orderBy = limitStr;
//            }
//        } else {
//            if (limitStr != null) {
//                orderBy += " " + limitStr;
//            }
//        }

        ContentResolver cr = context.getContentResolver();
//        Uri uri = Uri.parse("content://mms-sms/complete-conversations");
//        Uri uri = Telephony.MmsSms.CONTENT_CONVERSATIONS_URI;
        Uri[] uris = new Uri[] {
                Telephony.Sms.Inbox.CONTENT_URI,
                Telephony.Sms.Sent.CONTENT_URI
        };

        for (Uri uri : uris) {
            Cursor c = cr.query(uri, null, selection,
                    null, null);
            if (c == null) {
                continue;
            }
            if (!c.moveToFirst()) {
                Log.d(TAG, uri.getPath() + " " + c.getCount() + " smss");
                c.close();
                continue;
            }

            int idxId = c.getColumnIndexOrThrow(Telephony.MmsSms._ID);
            int idxTime = c.getColumnIndexOrThrow(Telephony.Sms.DATE);
            int idxAddr = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS);
            int idxBody = c.getColumnIndexOrThrow(Telephony.Sms.BODY);
            int idxType = c.getColumnIndexOrThrow(Telephony.Sms.TYPE);
            int idxRead = c.getColumnIndexOrThrow(Telephony.Sms.READ);

            Sms row;
            do {
                String rowAddr = c.getString(idxAddr);
                row = new Sms() {{
                    id = c.getLong(idxId);
                    source = SOURCE_CONTENTPROVIDER;
                    addr = rowAddr;
                    body = c.getString(idxBody);
                    date = c.getLong(idxTime);
                    type = c.getInt(idxType);
                    read = c.getInt(idxRead) != 0;
                }};
                if (onEach != null) {
                    onEach.onCallback(row);
                }
                res.add(row);
            } while (c.moveToNext());

            Log.d(TAG, uri.getPath() + " " + c.getCount() + " smss type " + row.type +
                    " addr " + row.addr);

            c.close();
        }

        Collections.sort(res, new Comparator<Sms>() {
            @Override
            public int compare(Sms o1, Sms o2) {
                return o1.date > o2.date ? -1 : o1.date == o2.date ? 0 : 1;
            }
        });

        Log.d(TAG, "done fetching content resolver smss");

        return res;
    }

    public void markRead(Context context, Sms sms) {
        ContentResolver cr = context.getContentResolver();
        ContentValues cv = new ContentValues();
        cv.put(Telephony.Sms.SEEN, true);
        cv.put(Telephony.Sms.READ, true);
        try {
            cr.update(ContentUris.withAppendedId(Telephony.MmsSms.CONTENT_URI, sms.id), cv,
                    Telephony.Sms.READ + " = 0", null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
