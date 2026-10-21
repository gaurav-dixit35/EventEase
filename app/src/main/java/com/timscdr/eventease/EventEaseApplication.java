package com.timscdr.eventease;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import com.timscdr.eventease.repository.LocalSettingsRepository;
import com.timscdr.eventease.repository.SettingsRepository;

public final class EventEaseApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        String mode = new LocalSettingsRepository(getSharedPreferences("eventease_settings", MODE_PRIVATE)).themeMode();
        AppCompatDelegate.setDefaultNightMode(SettingsRepositoryMode.toDelegate(mode));
    }

    private static final class SettingsRepositoryMode {
        static int toDelegate(String mode) {
            if ("light".equals(mode)) return AppCompatDelegate.MODE_NIGHT_NO;
            if ("dark".equals(mode)) return AppCompatDelegate.MODE_NIGHT_YES;
            return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }
}
