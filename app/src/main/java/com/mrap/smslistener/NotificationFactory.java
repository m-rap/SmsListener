package com.mrap.smslistener;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class NotificationFactory {
    final static String NOTIF_CHANNEL_ID = "mrapsmslistener";
    private static final boolean USE_ICON = true;

    public static void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String packageName = context.getPackageName();
            CharSequence name = packageName + "_notif_channel_name";
            String description = packageName + "notif_channel_desc";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIF_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            //NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static Notification createNotification(Context context, Class<? extends Context> onTapActivity, int notificationId, String title, String content) {
        createNotificationChannel(context);

        Resources res = context.getResources();
        String packageName = context.getPackageName();

        Intent onTapIntent = new Intent(context, onTapActivity);
        PendingIntent onTapPendingIntent = PendingIntent.getActivity(context, notificationId, onTapIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIF_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                //.setLargeIcon(((BitmapDrawable) context.getResources().getDrawable(R.drawable.app_icon)).getBitmap())
                //.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(onTapPendingIntent)
                ;

        if (USE_ICON) {
            int smallIconResId = res.getIdentifier("ic_notification", "drawable", packageName);
            if (smallIconResId == 0) {
                smallIconResId = res.getIdentifier("ic_launcher", "mipmap", packageName);
            }
            builder.setSmallIcon(smallIconResId);
        }

        return builder.build();
    }
}
