package com.timscdr.eventease.ui;

import com.timscdr.eventease.data.database.AppDatabase;
import com.timscdr.eventease.repository.AppRepositories;
import java.lang.reflect.Field;
import org.robolectric.RuntimeEnvironment;

/** Robolectric resets SQLite between tests but application singleton fields can survive. */
final class RepositoryIsolation {
    private RepositoryIsolation() { }
    static void reset() throws Exception {
        for (Class<?> type : new Class<?>[]{AppRepositories.class, AppDatabase.class}) {
            Field field = type.getDeclaredField("instance");
            field.setAccessible(true);
            field.set(null, null);
        }
        RuntimeEnvironment.getApplication().deleteDatabase("eventease.db");
        RuntimeEnvironment.getApplication().getSharedPreferences("eventease_session", 0).edit().clear().commit();
    }
}
