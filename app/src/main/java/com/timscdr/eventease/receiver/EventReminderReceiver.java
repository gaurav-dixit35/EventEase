package com.timscdr.eventease.receiver;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.timscdr.eventease.R;
import com.timscdr.eventease.util.AndroidEventReminderScheduler;

public final class EventReminderReceiver extends BroadcastReceiver {
    public static final String EXTRA_CODE = "registration_code", EXTRA_TITLE = "event_title", EXTRA_VENUE = "event_venue";
    @Override public void onReceive(Context context, Intent intent) {
        if (!context.getSharedPreferences("eventease_settings", Context.MODE_PRIVATE)
                .getBoolean("notifications_enabled", false)) return;
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;
        String title = intent.getStringExtra(EXTRA_TITLE);
        if (title == null || title.isEmpty()) return;
        String venue = intent.getStringExtra(EXTRA_VENUE);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context,
                AndroidEventReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_event)
                .setContentTitle(context.getString(R.string.event_reminder_title))
                .setContentText(context.getString(R.string.event_reminder_message, title))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(context.getString(
                        R.string.event_reminder_details, title, venue == null ? "" : venue)))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        String code = intent.getStringExtra(EXTRA_CODE);
        if (code == null) return;
        int notificationId = code.hashCode() & 0x7fffffff;
        NotificationManagerCompat.from(context).notify(notificationId, notification.build());
    }
}
