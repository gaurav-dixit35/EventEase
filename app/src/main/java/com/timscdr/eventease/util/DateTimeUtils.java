package com.timscdr.eventease.util;

import android.content.Context;
import android.text.format.DateFormat;
import java.util.Date;

public final class DateTimeUtils {
    private DateTimeUtils() { }
    public static String date(Context context, long millis) {
        return DateFormat.getMediumDateFormat(context).format(new Date(millis));
    }
    public static String time(Context context, long millis) {
        return DateFormat.getTimeFormat(context).format(new Date(millis));
    }
    public static String dateTime(Context context, long millis) {
        return date(context, millis) + " · " + time(context, millis);
    }
}
