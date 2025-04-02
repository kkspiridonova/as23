package com.example.everydaynik2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: Уведомление получено!");

        String title = intent.getStringExtra("title");
        String description = intent.getStringExtra("description");
        Log.d(TAG, "onReceive: title = " + title + ", description = " + description);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("event_channel", "Event Notifications", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Канал для уведомлений о событиях");
            manager.createNotificationChannel(channel);
            Log.d(TAG, "onReceive: Notification channel created");
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "event_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(description)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        int notifyId = (int) System.currentTimeMillis();
        Log.d(TAG, "onReceive: notifyId = " + notifyId);
        manager.notify(notifyId, builder.build());
        Log.d(TAG, "onReceive: Notification sent");
    }
}
