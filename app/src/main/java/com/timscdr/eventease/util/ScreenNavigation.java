package com.timscdr.eventease.util;

import android.app.Activity;
import android.content.Intent;
import androidx.core.app.ActivityOptionsCompat;
import com.timscdr.eventease.ui.auth.LoginActivity;
import com.timscdr.eventease.ui.dashboard.DashboardActivity;
import com.timscdr.eventease.ui.organizer.OrganizerDashboardActivity;
import com.timscdr.eventease.ui.organizer.OrganizerLoginActivity;

public final class ScreenNavigation {
    private ScreenNavigation() { }
    public static void home(Activity activity) { replaceTask(activity, DashboardActivity.class); }
    public static void login(Activity activity) { replaceTask(activity, LoginActivity.class); }
    public static void organizerHome(Activity activity) { replaceTask(activity, OrganizerDashboardActivity.class); }
    public static void organizerLogin(Activity activity) { replaceTask(activity, OrganizerLoginActivity.class); }

    private static void replaceTask(Activity activity, Class<?> destination) {
        activity.startActivity(new Intent(activity, destination)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK),
                ActivityOptionsCompat.makeCustomAnimation(activity, android.R.anim.fade_in,
                        android.R.anim.fade_out).toBundle());
        activity.finish();
    }
}
