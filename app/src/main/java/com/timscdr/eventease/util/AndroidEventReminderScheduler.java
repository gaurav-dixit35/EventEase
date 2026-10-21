package com.timscdr.eventease.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationManagerCompat;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.data.entity.Registration;
import com.timscdr.eventease.receiver.EventReminderReceiver;

/** One local reminder per registration, scheduled one hour before its event. */
public final class AndroidEventReminderScheduler implements EventReminderScheduler {
    public static final String CHANNEL_ID = "eventease_event_reminders";
    private static final long REMINDER_OFFSET = 60L * 60L * 1000L;
    private final Context app;
    public AndroidEventReminderScheduler(Context context) { app = context.getApplicationContext(); }

    @Override public void schedule(Registration registration, Event event) {
        cancel(registration.registrationCode);
        long triggerAt = event.dateTime - REMINDER_OFFSET;
        if (triggerAt <= System.currentTimeMillis()) return;
        createChannel();
        AlarmManager alarms = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (alarms != null) alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt,
                pending(registration.registrationCode, event.title, event.venue, PendingIntent.FLAG_UPDATE_CURRENT));
    }

    @Override public void cancel(String registrationCode) {
        if (registrationCode == null || registrationCode.isEmpty()) return;
        PendingIntent alarm = pending(registrationCode, "", "", PendingIntent.FLAG_NO_CREATE);
        if (alarm == null) return;
        AlarmManager alarms = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (alarms != null) alarms.cancel(alarm);
        alarm.cancel();
    }

    private PendingIntent pending(String code, String title, String venue, int extraFlags) {
        Intent intent = new Intent(app, EventReminderReceiver.class)
                .setAction("com.timscdr.eventease.EVENT_REMINDER")
                .setData(Uri.parse("eventease://reminder/" + Uri.encode(code)))
                .putExtra(EventReminderReceiver.EXTRA_CODE, code)
                .putExtra(EventReminderReceiver.EXTRA_TITLE, title)
                .putExtra(EventReminderReceiver.EXTRA_VENUE, venue);
        int flags = extraFlags | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(app, 0, intent, flags);
    }

    private void createChannel() {
        NotificationManagerCompat.from(app).createNotificationChannel(new NotificationChannelCompat.Builder(
                CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(app.getString(com.timscdr.eventease.R.string.reminder_channel_name)).build());
    }
}
